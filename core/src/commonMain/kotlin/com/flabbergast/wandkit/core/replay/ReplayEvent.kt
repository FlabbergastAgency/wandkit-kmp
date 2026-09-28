package com.flabbergast.wandkit.core.replay

import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Touch phases in their `wandkit-replay` v1 wire spelling (`p` on a `touch`
 * line).
 */
internal enum class ReplayTouchPhase(val wireValue: String) {
    DOWN("down"),
    MOVE("move"),
    UP("up"),
    CANCEL("cancel"),
}

/**
 * One entry in the replay buffer: a screenshot frame, a touch, a tracked
 * event, or a recorded pause. Mirrors the iOS SDK's `WandKitReplayEvent`; see
 * the iOS repo's `plans/session-replay.md` ("Wire format: wandkit-replay v1")
 * for the on-wire shape each of these encodes to.
 *
 * A [Frame] does not carry its JPEG bytes: they live in a [ReplayFrameStore]
 * (on disk by default on Android) and [Frame.ref] points at them, so the
 * buffer itself only ever holds a few dozen bytes per entry.
 */
internal sealed interface ReplayEvent {
    /**
     * Approximate cost used for the buffer's byte cap - the JPEG size for a
     * frame, a flat 64 for everything else, same as iOS. Counted whether the
     * bytes live on the heap or on disk, so the cap means the same thing
     * either way and bounds the upload size too.
     */
    val byteCost: Int

    data class Frame(
        /** The JPEG's pixel dimensions (can differ from the header after a rotation). */
        val width: Int,
        val height: Int,
        val ref: ReplayFrameRef,
    ) : ReplayEvent {
        override val byteCost: Int get() = ref.byteSize + FLAT_COST
    }

    /** [x]/[y] are in dp, in the same space as the header's `width`x`height`. */
    data class Touch(
        val phase: ReplayTouchPhase,
        val x: Double,
        val y: Double,
    ) : ReplayEvent {
        override val byteCost: Int get() = FLAT_COST
    }

    /**
     * A `WandKit.event(...)` call; [data] mirrors its properties, if any. A
     * JSON object rather than a string map because the wire format allows any
     * JSON values (KMP's `event` only ever produces strings - see [fromProperties]).
     */
    data class Event(
        val name: String,
        val data: JsonObject?,
    ) : ReplayEvent {
        override val byteCost: Int get() = FLAT_COST

        companion object {
            /** `null` data for no properties, like iOS. */
            fun fromProperties(name: String, properties: Map<String, String>): Event = Event(
                name = name,
                data = properties.takeIf { it.isNotEmpty() }
                    ?.let { props -> JsonObject(props.mapValues { (_, value) -> JsonPrimitive(value) }) },
            )
        }
    }

    /** Emitted at the moment the recorder resumes; [millis] is how long it was paused. */
    data class Gap(
        val millis: Long,
    ) : ReplayEvent {
        override val byteCost: Int get() = FLAT_COST
    }

    companion object {
        const val FLAT_COST: Int = 64
    }
}

internal val ReplayEvent.isFrame: Boolean
    get() = this is ReplayEvent.Frame

/**
 * The first line of a `wandkit-replay` NDJSON file. Everything but
 * [startEpochMillis] is taken as given; [ReplayBuffer.freeze] replaces it with
 * the oldest retained frame's timestamp.
 */
internal data class ReplayHeader(
    val startEpochMillis: Long,
    /** The window's size in dp - the Android counterpart of iOS points. */
    val width: Int,
    val height: Int,
    /** Display density - informational only, frames carry their own pixel dimensions. */
    val scale: Double,
    /** Same object the posts session is minted with, so the dashboard reads it the same way. */
    val device: SdkPostsSessionDeviceDto,
    /** `android-<version>`, the Android counterpart of iOS's `ios-<semver>`. */
    val sdk: String,
)

/** A buffered event with its wall-clock timestamp relative to the header's `start`. */
internal data class ReplayTimedEvent(
    /** Milliseconds since the header's `start`; never negative, non-decreasing. */
    val t: Long,
    val event: ReplayEvent,
)
