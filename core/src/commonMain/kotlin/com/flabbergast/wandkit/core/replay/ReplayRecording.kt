package com.flabbergast.wandkit.core.replay

/**
 * A frozen, immutable recording ready to upload as a `replay` attachment -
 * the NDJSON bytes (in memory or in a file, see [ReplayRecordingSource]) plus
 * a little metadata for logging. Produced by [ReplaySnapshot.encode].
 *
 * Owned by the screenshot prompt it rides on: [discard] once the report is
 * sent or the card is dismissed, so an on-disk recording never outlives its
 * card.
 */
internal class ReplayRecording(
    private val source: ReplayRecordingSource,
    /** The oldest retained frame's timestamp - what the wire format's `t: 0` is relative to. */
    val startEpochMillis: Long,
    /** The last event's `t`. */
    val durationMillis: Long,
    val frameCount: Int,
    val eventCount: Int,
) {
    val sizeBytes: Long get() = source.sizeBytes

    fun readBytes(): ByteArray? = source.readBytes()

    fun discard() = source.discard()

    override fun toString(): String =
        "ReplayRecording(frames=$frameCount, events=$eventCount, bytes=$sizeBytes, durationMs=$durationMillis)"

    companion object {
        const val CONTENT_TYPE: String = "application/x-ndjson"
        const val FILE_NAME: String = "replay.ndjson"

        /** The attachment kind the API stores replays under - dashboard-only, like `debug`. */
        const val ATTACHMENT_KIND: String = "replay"
    }
}

/**
 * The buffer's contents at the moment a screenshot was taken, detached from
 * the live buffer by [ReplayBuffer.freeze]: the timed events plus the frame
 * store that holds their JPEGs, which this snapshot now owns.
 *
 * [freeze][ReplayBuffer.freeze] itself is cheap (it runs on the main thread,
 * before the report card appears); the expensive part - reading every frame
 * back and base64-encoding it - happens in [encode], off the main thread.
 */
internal class ReplaySnapshot(
    private val header: ReplayHeader,
    private val events: List<ReplayTimedEvent>,
    private val store: ReplayFrameStore,
) {
    val frameCount: Int get() = events.count { it.event.isFrame }

    /**
     * Encodes the snapshot into a [ReplayRecording] and destroys the frame
     * store either way. `null` when no frame could be read back (nothing to
     * play) or writing the output failed.
     */
    fun encode(): ReplayRecording? {
        val output = store.createRecordingOutput()
        return try {
            val result = ReplayEncoder.encode(
                header = header,
                events = events,
                readFrame = store::read,
                output = output,
            )
            if (result.frameCount == 0) {
                output.abort()
                null
            } else {
                output.finish()?.let { source ->
                    ReplayRecording(
                        source = source,
                        startEpochMillis = header.startEpochMillis,
                        durationMillis = result.lastT,
                        frameCount = result.frameCount,
                        eventCount = result.eventCount,
                    )
                }
            }
        } catch (e: Exception) {
            output.abort()
            null
        } finally {
            store.destroy()
        }
    }

    /** Throws the snapshot away without encoding it. */
    fun discard() = store.destroy()
}
