package com.flabbergast.wandkit.core.replay

/**
 * A ring buffer of [ReplayEvent]s bounded by both time and bytes - a port of
 * the iOS SDK's `WandKitReplayBuffer` with one difference: frame bytes live in
 * a [ReplayFrameStore] (on disk by default on Android), so the buffer only
 * holds metadata and tells the store to free a frame when it evicts it.
 *
 * Not thread-safe by design - the Android recorder confines every call to the
 * main thread, like the rest of the screenshot-report pipeline.
 */
internal class ReplayBuffer(
    windowSeconds: Int,
    maxBytes: Int,
    private val storeFactory: () -> ReplayFrameStore,
) {
    private class StoredEvent(
        val atMillis: Long,
        val event: ReplayEvent,
    )

    private val windowMillis: Long = maxOf(windowSeconds, MIN_WINDOW_SECONDS) * 1_000L
    private val maxBytes: Int = maxOf(maxBytes, MIN_MAX_BYTES)
    private val events = ArrayList<StoredEvent>()
    private var totalBytes = 0

    /** Where frames appended to this buffer must be [put][ReplayFrameStore.put]. Replaced on [removeAll] and [freeze]. */
    var store: ReplayFrameStore = storeFactory()
        private set

    val isEmpty: Boolean get() = events.isEmpty()

    val eventCount: Int get() = events.size

    val byteCount: Int get() = totalBytes

    val frameCount: Int get() = events.count { it.event.isFrame }

    /**
     * Appends [event], then evicts: everything older than
     * `atMillis - windowSeconds`, then oldest-first until
     * `byteCount <= maxBytes`. When either eviction removes a frame, every
     * touch older than the new oldest remaining frame is dropped too - a touch
     * with no frame to anchor it to is useless to the player. Tracked events
     * are never evicted that way; [freeze] is the only place events before the
     * first frame are dropped.
     *
     * A single event whose own cost exceeds `maxBytes` is dropped instead of
     * appended (and, for a frame, freed in the store).
     *
     * @return `false` when the event was dropped.
     */
    fun append(event: ReplayEvent, atMillis: Long): Boolean {
        val cost = event.byteCost
        if (cost > maxBytes) {
            free(event)
            return false
        }

        // Ordered insert, not a plain append: a frame reaches the buffer only
        // after its background JPEG encode, stamped with its capture time, so
        // touches and events recorded in the meantime are already here with
        // later times. Scanning from the back keeps the common case O(1).
        var insertionIndex = events.size
        while (insertionIndex > 0 && events[insertionIndex - 1].atMillis > atMillis) {
            insertionIndex -= 1
        }
        events.add(insertionIndex, StoredEvent(atMillis, event))
        totalBytes += cost

        var didEvictFrame = false

        // Age never evicts the newest frame: on a static screen the deduper
        // appends nothing new, so without this the one frame everything hangs
        // off would age out and freeze would have nothing to play. The byte
        // cap below can still take it.
        val cutoff = atMillis - windowMillis
        while (events.isNotEmpty() && events[0].atMillis < cutoff && !isNewestFrame(0)) {
            didEvictFrame = evictOldest() || didEvictFrame
        }

        while (totalBytes > maxBytes && events.isNotEmpty()) {
            didEvictFrame = evictOldest() || didEvictFrame
        }

        if (didEvictFrame) {
            val newOldestFrameAt = events.firstOrNull { it.event.isFrame }?.atMillis
            if (newOldestFrameAt == null) {
                // No frame survived eviction - any touch left is orphaned.
                dropTouches { true }
            } else {
                dropTouches { it < newOldestFrameAt }
            }
        }
        return events.any { it.event === event }
    }

    /** Drops everything and starts over with a fresh store; the old one is destroyed. */
    fun removeAll() {
        events.clear()
        totalBytes = 0
        store.destroy()
        store = storeFactory()
    }

    /** Stops the buffer for good: frees the store without creating another one. */
    fun close() {
        events.clear()
        totalBytes = 0
        store.destroy()
    }

    /**
     * Detaches the buffer's contents into a [ReplaySnapshot], or returns `null`
     * (leaving the buffer untouched) when it holds no frame - there is nothing
     * to play back.
     *
     * Drops every event before the oldest retained frame, so the first line
     * after the header is always a frame. `t` is milliseconds since that
     * frame; the header's `start` is replaced with its timestamp.
     *
     * Unlike iOS, the buffer is left empty afterwards: the snapshot takes the
     * frame store with it (so frames it still has to read can't be evicted
     * from under it), and the buffer continues with a fresh store. The next
     * report therefore starts its lead-up from this screenshot.
     */
    fun freeze(header: ReplayHeader): ReplaySnapshot? {
        val firstFrameIndex = events.indexOfFirst { it.event.isFrame }
        if (firstFrameIndex < 0) return null

        val retained = events.subList(firstFrameIndex, events.size)
        val start = retained.first().atMillis
        val timed = retained.map { stored ->
            ReplayTimedEvent(t = maxOf(0L, stored.atMillis - start), event = stored.event)
        }

        val snapshot = ReplaySnapshot(
            header = header.copy(startEpochMillis = start),
            events = timed,
            store = store,
        )

        events.clear()
        totalBytes = 0
        store = storeFactory()
        return snapshot
    }

    private fun isNewestFrame(index: Int): Boolean {
        if (!events[index].event.isFrame) return false
        for (i in index + 1 until events.size) {
            if (events[i].event.isFrame) return false
        }
        return true
    }

    /** @return whether the evicted event was a frame. */
    private fun evictOldest(): Boolean {
        if (events.isEmpty()) return false
        val removed = events.removeAt(0)
        totalBytes -= removed.event.byteCost
        free(removed.event)
        return removed.event.isFrame
    }

    private fun free(event: ReplayEvent) {
        if (event is ReplayEvent.Frame) store.delete(event.ref)
    }

    private inline fun dropTouches(shouldDrop: (Long) -> Boolean) {
        val iterator = events.iterator()
        while (iterator.hasNext()) {
            val stored = iterator.next()
            if (stored.event is ReplayEvent.Touch && shouldDrop(stored.atMillis)) {
                totalBytes -= stored.event.byteCost
                iterator.remove()
            }
        }
    }

    private companion object {
        /** A window lower than this is nonsensical - a recorder racing down to zero would just spin. */
        const val MIN_WINDOW_SECONDS = 1

        /** A cap this low couldn't hold even one frame's worth of history. */
        const val MIN_MAX_BYTES = 256 * 1024
    }
}
