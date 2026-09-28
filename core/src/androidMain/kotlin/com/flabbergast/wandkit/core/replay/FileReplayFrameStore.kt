package com.flabbergast.wandkit.core.replay

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * On-disk layout for session replay, under the app's private cache directory:
 *
 * ```
 * cache/wandkit-replay/
 *   session-<n>/<n>.jpg      one directory per buffer session (frames)
 *   recordings/replay-<n>.ndjson   frozen recordings awaiting upload
 * ```
 *
 * `cacheDir` rather than `filesDir`: it is app-private, excluded from Auto
 * Backup, and the OS may reclaim it under storage pressure - all fine for data
 * that is meant to be short-lived anyway. Nothing here outlives the process:
 * [sweepOnce] deletes the whole tree the first time a recorder starts.
 */
internal class ReplayDiskStorage private constructor(
    private val root: File,
    private val io: Executor,
) {
    private val sessionCounter = AtomicLong()
    private val recordingsDir = File(root, "recordings")

    fun newFrameStore(): ReplayFrameStore = FileReplayFrameStore(
        sessionDir = File(root, "session-${System.currentTimeMillis()}-${sessionCounter.incrementAndGet()}"),
        recordingsDir = recordingsDir,
        io = io,
    )

    companion object {
        private const val DIRECTORY_NAME = "wandkit-replay"
        private val swept = AtomicBoolean(false)

        fun forContext(context: Context, io: Executor = ReplayIo): ReplayDiskStorage {
            val storage = ReplayDiskStorage(File(context.cacheDir, DIRECTORY_NAME), io)
            storage.sweepOnce()
            return storage
        }

        internal fun forDirectory(root: File, io: Executor): ReplayDiskStorage = ReplayDiskStorage(root, io)
    }

    /**
     * Deletes whatever a previous process left behind (a crash mid-session, a
     * recording that never got sent). Queued on [io] before any write of this
     * process, so it can never race one. Only the first call per process does
     * anything - later sessions' directories must survive a reconfigure.
     */
    internal fun sweepOnce() {
        if (!swept.compareAndSet(false, true)) return
        io.execute { root.deleteRecursively() }
    }
}

/**
 * A [ReplayFrameStore] that writes each frame to its own file in
 * [sessionDir]. The ref holds the file, so reads need no index and there is no
 * shared map to guard across threads.
 */
internal class FileReplayFrameStore(
    private val sessionDir: File,
    private val recordingsDir: File,
    private val io: Executor,
) : ReplayFrameStore {
    private class FileFrameRef(val file: File, override val byteSize: Int) : ReplayFrameRef

    private val frameCounter = AtomicLong()
    private val recordingCounter = AtomicLong()

    @Volatile
    private var destroyed = false

    override val isPersistent: Boolean get() = true

    override fun put(jpeg: ByteArray): ReplayFrameRef? {
        if (destroyed) return null
        return try {
            if (!sessionDir.isDirectory && !sessionDir.mkdirs()) return null
            val file = File(sessionDir, "${frameCounter.incrementAndGet()}.jpg")
            file.writeBytes(jpeg)
            if (destroyed) {
                // Destroyed mid-write; the queued directory delete would catch
                // it anyway, but don't hand out a ref to a dying file.
                file.delete()
                null
            } else {
                FileFrameRef(file, jpeg.size)
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    override fun read(ref: ReplayFrameRef): ByteArray? {
        val file = (ref as? FileFrameRef)?.file ?: return null
        return try {
            file.readBytes()
        } catch (e: IOException) {
            null
        }
    }

    override fun delete(ref: ReplayFrameRef) {
        val file = (ref as? FileFrameRef)?.file ?: return
        io.execute { file.delete() }
    }

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        io.execute { sessionDir.deleteRecursively() }
    }

    override fun createRecordingOutput(): ReplayRecordingOutput = FileRecordingOutput(
        file = File(recordingsDir, "replay-${System.currentTimeMillis()}-${recordingCounter.incrementAndGet()}.ndjson"),
        io = io,
    )
}

/**
 * Streams an encoded recording straight to a file, so the 2-5 MB of base64 a
 * recording weighs never has to exist on the heap in one piece. Any I/O error
 * poisons the output: [finish] then returns `null` and the file is removed.
 */
internal class FileRecordingOutput(
    private val file: File,
    private val io: Executor,
) : ReplayRecordingOutput {
    private var stream: OutputStream? = null
    private var failed = false
    private var written = 0L

    override fun write(bytes: ByteArray) {
        if (failed) return
        try {
            val out = stream ?: run {
                file.parentFile?.mkdirs()
                BufferedOutputStream(FileOutputStream(file), BUFFER_SIZE).also { stream = it }
            }
            out.write(bytes)
            written += bytes.size
        } catch (e: IOException) {
            failed = true
        }
    }

    override fun finish(): ReplayRecordingSource? {
        val closed = try {
            stream?.close()
            true
        } catch (e: IOException) {
            false
        }
        stream = null
        if (failed || !closed || written == 0L) {
            file.delete()
            return null
        }
        return FileRecordingSource(file, written, io)
    }

    override fun abort() {
        runCatching { stream?.close() }
        stream = null
        file.delete()
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}

internal class FileRecordingSource(
    private val file: File,
    override val sizeBytes: Long,
    private val io: Executor,
) : ReplayRecordingSource {
    private val discarded = AtomicBoolean(false)

    override fun readBytes(): ByteArray? {
        if (discarded.get()) return null
        return try {
            file.readBytes()
        } catch (e: IOException) {
            null
        }
    }

    override fun discard() {
        if (!discarded.compareAndSet(false, true)) return
        io.execute { file.delete() }
    }
}
