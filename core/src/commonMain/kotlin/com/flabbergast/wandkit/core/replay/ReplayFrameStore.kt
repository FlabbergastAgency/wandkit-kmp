package com.flabbergast.wandkit.core.replay

/**
 * A handle to one frame's JPEG bytes inside a [ReplayFrameStore]. Opaque to
 * everything but the store that minted it.
 */
internal interface ReplayFrameRef {
    val byteSize: Int
}

/**
 * Where a replay buffer keeps its frames' JPEG bytes, and where a frozen
 * recording is written.
 *
 * Two implementations: [InMemoryReplayFrameStore] (bytes on the heap, like the
 * iOS SDK) and, on Android, a file-backed store under the app's cache
 * directory. One store instance belongs to one buffer "session": when the
 * buffer is wiped (app backgrounded) or detached into a
 * [ReplaySnapshot] (screenshot taken), the old store is [destroy]ed as a whole
 * and the buffer starts over with a fresh one. That keeps in-flight writes from
 * a previous session from ever landing in the next one.
 *
 * Threading: [put] runs on the recorder's encode thread; [read] runs on
 * whatever thread encodes a snapshot; [delete] and [destroy] may be called from
 * the main thread and must not block it on I/O.
 */
internal interface ReplayFrameStore {
    /** `true` when frames are written to disk rather than held in memory. */
    val isPersistent: Boolean

    /** Stores [jpeg]; `null` when the store was already destroyed or the write failed. */
    fun put(jpeg: ByteArray): ReplayFrameRef?

    /** The bytes behind [ref], or `null` if they are gone (deleted, write lost). */
    fun read(ref: ReplayFrameRef): ByteArray?

    /** Frees one frame, after the buffer evicted it. */
    fun delete(ref: ReplayFrameRef)

    /** Frees every frame. Afterwards [put] always returns `null`. */
    fun destroy()

    /** A fresh sink for an encoded recording; independent of this store's own lifetime. */
    fun createRecordingOutput(): ReplayRecordingOutput
}

/** Receives an NDJSON recording as it is encoded, line by line. */
internal interface ReplayRecordingOutput {
    fun write(bytes: ByteArray)

    /** Closes the output; `null` when something went wrong while writing. */
    fun finish(): ReplayRecordingSource?

    /** Throws away whatever was written. */
    fun abort()
}

/** The bytes of a finished recording, wherever they live. */
internal interface ReplayRecordingSource {
    val sizeBytes: Long

    /** Loads the whole recording for upload; `null` if it is gone. */
    fun readBytes(): ByteArray?

    /** Frees the recording. Idempotent. */
    fun discard()
}

/**
 * Keeps frames on the heap. The ref *is* the bytes, so there is no shared
 * mutable state to guard across the encode and main threads, and [delete] is
 * just letting go of the ref.
 */
internal class InMemoryReplayFrameStore : ReplayFrameStore {
    private class MemoryFrameRef(val bytes: ByteArray) : ReplayFrameRef {
        override val byteSize: Int get() = bytes.size
    }

    // Written on the main thread, read on the encode thread; a stale read
    // costs at most one frame that the buffer's generation check discards.
    private var destroyed = false

    override val isPersistent: Boolean get() = false

    override fun put(jpeg: ByteArray): ReplayFrameRef? = if (destroyed) null else MemoryFrameRef(jpeg)

    override fun read(ref: ReplayFrameRef): ByteArray? = (ref as? MemoryFrameRef)?.bytes

    override fun delete(ref: ReplayFrameRef) = Unit

    override fun destroy() {
        destroyed = true
    }

    override fun createRecordingOutput(): ReplayRecordingOutput = InMemoryRecordingOutput()
}

internal class InMemoryRecordingOutput : ReplayRecordingOutput {
    private val chunks = mutableListOf<ByteArray>()
    private var size = 0

    override fun write(bytes: ByteArray) {
        chunks += bytes
        size += bytes.size
    }

    override fun finish(): ReplayRecordingSource {
        val data = ByteArray(size)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(data, offset)
            offset += chunk.size
        }
        chunks.clear()
        return InMemoryRecordingSource(data)
    }

    override fun abort() {
        chunks.clear()
        size = 0
    }
}

internal class InMemoryRecordingSource(private var data: ByteArray?) : ReplayRecordingSource {
    override val sizeBytes: Long = data?.size?.toLong() ?: 0L

    override fun readBytes(): ByteArray? = data

    override fun discard() {
        data = null
    }
}
