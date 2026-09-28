package com.flabbergast.wandkit.core.replay

/** An in-memory store that records what the buffer asked it to free. */
internal class TrackingFrameStore : ReplayFrameStore {
    private val delegate = InMemoryReplayFrameStore()
    val deleted = mutableListOf<ReplayFrameRef>()
    var destroyed = false
        private set

    override val isPersistent: Boolean get() = false

    override fun put(jpeg: ByteArray): ReplayFrameRef? = if (destroyed) null else delegate.put(jpeg)

    override fun read(ref: ReplayFrameRef): ByteArray? = if (ref in deleted) null else delegate.read(ref)

    override fun delete(ref: ReplayFrameRef) {
        deleted += ref
    }

    override fun destroy() {
        destroyed = true
        delegate.destroy()
    }

    override fun createRecordingOutput(): ReplayRecordingOutput = delegate.createRecordingOutput()
}

internal fun TrackingFrameStore.frame(size: Int, width: Int = 10, height: Int = 20): ReplayEvent.Frame =
    ReplayEvent.Frame(width, height, requireNotNull(put(ByteArray(size))))
