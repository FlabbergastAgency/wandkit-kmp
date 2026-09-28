package com.flabbergast.wandkit.core.replay

import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import java.io.File
import java.util.concurrent.Executor
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileReplayFrameStoreTest {
    private val root: File = createTempDirectory("wandkit-replay-test").toFile()

    /** Runs "background" work inline so every assertion sees its effect. */
    private val direct = Executor { it.run() }

    private val storage = ReplayDiskStorage.forDirectory(root, direct)

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun jpegFilesUnder(dir: File) = dir.walkTopDown().filter { it.isFile && it.extension == "jpg" }.toList()

    @Test
    fun framesAreWrittenToDiskAndReadBack() {
        val store = storage.newFrameStore()
        val bytes = byteArrayOf(1, 2, 3, 4)

        val ref = assertNotNull(store.put(bytes))

        assertTrue(store.isPersistent)
        assertEquals(4, ref.byteSize)
        assertContentEquals(bytes, store.read(ref))
        assertEquals(1, jpegFilesUnder(root).size)
    }

    @Test
    fun deleteRemovesTheFrameFile() {
        val store = storage.newFrameStore()
        val ref = assertNotNull(store.put(byteArrayOf(1)))

        store.delete(ref)

        assertNull(store.read(ref))
        assertTrue(jpegFilesUnder(root).isEmpty())
    }

    @Test
    fun destroyRemovesTheSessionAndRefusesLaterWrites() {
        val store = storage.newFrameStore()
        store.put(byteArrayOf(1))
        store.put(byteArrayOf(2))

        store.destroy()

        assertTrue(jpegFilesUnder(root).isEmpty())
        assertNull(store.put(byteArrayOf(3)))
    }

    @Test
    fun sessionsDoNotShareFiles() {
        val first = storage.newFrameStore()
        val second = storage.newFrameStore()
        val kept = assertNotNull(second.put(byteArrayOf(9)))
        first.put(byteArrayOf(1))

        first.destroy()

        assertContentEquals(byteArrayOf(9), second.read(kept))
    }

    @Test
    fun snapshotStreamsTheRecordingToAFileThatOutlivesTheFrames() {
        val buffer = ReplayBuffer(windowSeconds = 60, maxBytes = 4 * 1024 * 1024) { storage.newFrameStore() }
        buffer.append(ReplayEvent.Frame(2, 2, assertNotNull(buffer.store.put(byteArrayOf(7, 7)))), atMillis = 1_000)
        buffer.append(ReplayEvent.Touch(ReplayTouchPhase.DOWN, 1.0, 1.0), atMillis = 1_100)

        val header = ReplayHeader(0, 1, 1, 1.0, SdkPostsSessionDeviceDto(platform = "android"), "android-test")
        val recording = assertNotNull(assertNotNull(buffer.freeze(header)).encode())

        // Frames are gone with their session; only the NDJSON remains.
        assertTrue(jpegFilesUnder(root).isEmpty())
        val ndjsonFiles = root.walkTopDown().filter { it.isFile && it.extension == "ndjson" }.toList()
        assertEquals(1, ndjsonFiles.size)

        val text = assertNotNull(recording.readBytes()).decodeToString()
        assertEquals(3, text.trimEnd('\n').split('\n').size)
        assertEquals(text.length.toLong(), recording.sizeBytes)
        assertTrue(text.contains(""""jpeg":"Bwc=""""), text)

        recording.discard()
        assertFalse(ndjsonFiles.single().exists())
        assertNull(recording.readBytes())
    }

    @Test
    fun abortedRecordingLeavesNoFile() {
        val output = storage.newFrameStore().createRecordingOutput()
        output.write("partial".encodeToByteArray())

        output.abort()

        assertTrue(root.walkTopDown().none { it.isFile && it.extension == "ndjson" })
    }
}
