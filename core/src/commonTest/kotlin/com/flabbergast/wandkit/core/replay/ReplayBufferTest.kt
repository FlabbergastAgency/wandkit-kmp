package com.flabbergast.wandkit.core.replay

import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReplayBufferTest {
    private val stores = mutableListOf<TrackingFrameStore>()

    private fun buffer(windowSeconds: Int = 60, maxBytes: Int = 4 * 1024 * 1024) =
        ReplayBuffer(windowSeconds, maxBytes) { TrackingFrameStore().also { stores += it } }

    private val ReplayBuffer.trackingStore: TrackingFrameStore get() = store as TrackingFrameStore

    private fun header() = ReplayHeader(
        startEpochMillis = 0,
        width = 393,
        height = 852,
        scale = 3.0,
        device = SdkPostsSessionDeviceDto(platform = "android"),
        sdk = "android-test",
    )

    private val touch = ReplayEvent.Touch(ReplayTouchPhase.DOWN, 1.0, 2.0)

    @Test
    fun ageEvictsOldEventsButNeverTheNewestFrame() {
        val buffer = buffer(windowSeconds = 10)
        val oldFrame = buffer.trackingStore.frame(100)
        buffer.append(oldFrame, atMillis = 0)
        buffer.append(ReplayEvent.Event("early", null), atMillis = 1_000)

        // 60 s later nothing new was captured (static screen): the only frame
        // survives, the old event does not.
        buffer.append(ReplayEvent.Event("late", null), atMillis = 60_000)

        assertEquals(1, buffer.frameCount)
        val snapshot = assertNotNull(buffer.freeze(header()))
        assertEquals(1, snapshot.frameCount)
    }

    @Test
    fun ageEvictsAnOlderFrameOnceANewerOneExistsAndFreesIt() {
        val buffer = buffer(windowSeconds = 10)
        val store = buffer.trackingStore
        val first = store.frame(100)
        buffer.append(first, atMillis = 0)
        buffer.append(store.frame(100), atMillis = 20_000)

        assertEquals(1, buffer.frameCount)
        assertEquals(listOf(first.ref), store.deleted)
    }

    @Test
    fun byteCapEvictsOldestFirst() {
        val buffer = buffer(maxBytes = 256 * 1024)
        val store = buffer.trackingStore
        val frames = List(4) { store.frame(100 * 1024) }
        frames.forEachIndexed { index, frame -> buffer.append(frame, atMillis = index * 100L) }

        assertTrue(buffer.byteCount <= 256 * 1024)
        assertEquals(2, buffer.frameCount)
        assertEquals(listOf(frames[0].ref, frames[1].ref), store.deleted)
    }

    @Test
    fun frameEvictionDropsTouchesOlderThanTheNewOldestFrame() {
        val buffer = buffer(maxBytes = 256 * 1024)
        val store = buffer.trackingStore
        buffer.append(store.frame(100 * 1024), atMillis = 0)
        buffer.append(touch, atMillis = 100)
        buffer.append(ReplayEvent.Event("kept", null), atMillis = 150)
        buffer.append(store.frame(100 * 1024), atMillis = 200)
        buffer.append(touch, atMillis = 300)

        // Over the cap: only the first frame goes, which orphans the touch at
        // 100 - but not the event at 150, which is not a touch.
        buffer.append(store.frame(100 * 1024), atMillis = 400)
        assertEquals(4, buffer.eventCount, "event@150, frame@200, touch@300, frame@400")

        // freeze then drops the event too, as it precedes the first frame.
        val kinds = encodedLines(assertNotNull(buffer.freeze(header())))
            .map { it.substringAfter("\"e\":\"").substringBefore('"') }
        assertEquals(listOf("frame", "touch", "frame"), kinds)
    }

    @Test
    fun oversizedEventIsDroppedAndFreed() {
        val buffer = buffer(maxBytes = 256 * 1024)
        val store = buffer.trackingStore
        val huge = store.frame(300 * 1024)

        assertFalse(buffer.append(huge, atMillis = 0))

        assertTrue(buffer.isEmpty)
        assertEquals(listOf(huge.ref), store.deleted)
    }

    @Test
    fun lateFrameIsInsertedInTimeOrder() {
        val buffer = buffer()
        val store = buffer.trackingStore
        buffer.append(store.frame(10), atMillis = 0)
        buffer.append(touch, atMillis = 300)
        // Encoded in the background, so it arrives after a later touch.
        buffer.append(store.frame(10), atMillis = 200)

        val lines = encodedLines(assertNotNull(buffer.freeze(header())))
        assertEquals(listOf(0L, 200L, 300L), lines.map { it.substringAfter("\"t\":").substringBefore(',').toLong() })
    }

    @Test
    fun freezeWithoutAFrameReturnsNullAndKeepsTheBuffer() {
        val buffer = buffer()
        buffer.append(touch, atMillis = 0)

        assertNull(buffer.freeze(header()))
        assertEquals(1, buffer.eventCount)
        assertFalse(buffer.trackingStore.destroyed)
    }

    @Test
    fun freezeDropsEverythingBeforeTheFirstFrameAndRebasesT() {
        val buffer = buffer()
        val store = buffer.trackingStore
        buffer.append(ReplayEvent.Event("before", null), atMillis = 1_000)
        buffer.append(store.frame(10), atMillis = 2_000)
        buffer.append(ReplayEvent.Gap(500), atMillis = 2_750)

        val snapshot = assertNotNull(buffer.freeze(header()))
        val recording = assertNotNull(snapshot.encode())

        assertEquals(2_000L, recording.startEpochMillis)
        assertEquals(750L, recording.durationMillis)
        assertEquals(2, recording.eventCount)
        assertEquals(1, recording.frameCount)
    }

    @Test
    fun freezeDetachesTheStoreAndStartsOverWithAFreshOne() {
        val buffer = buffer()
        val originalStore = buffer.trackingStore
        buffer.append(originalStore.frame(10), atMillis = 0)

        val snapshot = assertNotNull(buffer.freeze(header()))

        assertTrue(buffer.isEmpty)
        assertEquals(0, buffer.byteCount)
        assertNotSame(originalStore, buffer.store)
        assertFalse(originalStore.destroyed, "The snapshot still has to read its frames")

        assertNotNull(snapshot.encode())
        assertTrue(originalStore.destroyed, "Encoding hands the store back for destruction")
    }

    @Test
    fun discardingASnapshotDestroysItsStore() {
        val buffer = buffer()
        val originalStore = buffer.trackingStore
        buffer.append(originalStore.frame(10), atMillis = 0)

        assertNotNull(buffer.freeze(header())).discard()

        assertTrue(originalStore.destroyed)
    }

    @Test
    fun removeAllDestroysTheStoreAndReplacesIt() {
        val buffer = buffer()
        val originalStore = buffer.trackingStore
        buffer.append(originalStore.frame(10), atMillis = 0)
        buffer.append(touch, atMillis = 10)

        buffer.removeAll()

        assertTrue(buffer.isEmpty)
        assertEquals(0, buffer.byteCount)
        assertTrue(originalStore.destroyed)
        assertNotSame(originalStore, buffer.store)
        assertSame(stores.last(), buffer.store)
    }

    @Test
    fun byteCountTracksFramesAndFlatCosts() {
        val buffer = buffer()
        buffer.append(buffer.trackingStore.frame(1_000), atMillis = 0)
        buffer.append(touch, atMillis = 1)

        assertEquals(1_000 + 64 + 64, buffer.byteCount)
    }

    private fun encodedLines(snapshot: ReplaySnapshot): List<String> {
        val recording = assertNotNull(snapshot.encode())
        val text = assertNotNull(recording.readBytes()).decodeToString()
        return text.trimEnd('\n').split('\n').drop(1)
    }
}
