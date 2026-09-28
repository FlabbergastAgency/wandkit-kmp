package com.flabbergast.wandkit.core.replay

import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.math.round

/**
 * Writes `wandkit-replay` v1 NDJSON: one JSON object per line, `"\n"`
 * terminated, header first. Byte-for-byte the same shape the iOS SDK's
 * `WandKitReplayEncoder` emits (key order aside, which readers ignore), so the
 * dashboard's rrweb transformer plays both. The shared reference fixture is
 * mirrored in `ReplayFixture.kt` in commonTest; change it together with the
 * iOS and dashboard copies.
 */
internal object ReplayEncoder {
    const val VERSION: Int = 1

    fun headerLine(header: ReplayHeader): String = buildJsonObject {
        put("v", VERSION)
        put("start", formatReplayStart(header.startEpochMillis))
        put("width", header.width)
        put("height", header.height)
        put("scale", numberValue(header.scale))
        put("device", deviceObject(header.device))
        put("sdk", header.sdk)
    }.toString()

    /**
     * One event line. [jpeg] must be the frame's bytes for a
     * [ReplayEvent.Frame] and is ignored otherwise.
     */
    fun eventLine(t: Long, event: ReplayEvent, jpeg: ByteArray? = null): String = when (event) {
        // Hand-built rather than through JsonObject: the base64 payload is by
        // far the biggest part of a recording, and base64's alphabet never
        // needs JSON escaping, so this skips one full copy of it.
        is ReplayEvent.Frame -> buildString {
            append("{\"t\":").append(t)
            append(",\"e\":\"frame\"")
            append(",\"w\":").append(event.width)
            append(",\"h\":").append(event.height)
            append(",\"jpeg\":\"").append(Base64.encode(requireNotNull(jpeg) { "A frame line needs its JPEG bytes" }))
            append("\"}")
        }

        is ReplayEvent.Touch -> buildJsonObject {
            put("t", t)
            put("e", "touch")
            put("p", event.phase.wireValue)
            put("x", numberValue(event.x))
            put("y", numberValue(event.y))
        }.toString()

        is ReplayEvent.Event -> buildJsonObject {
            put("t", t)
            put("e", "event")
            put("name", event.name)
            event.data?.let { data -> put("data", data) }
        }.toString()

        is ReplayEvent.Gap -> buildJsonObject {
            put("t", t)
            put("e", "gap")
            put("ms", event.millis)
        }.toString()
    }

    /**
     * Writes the header and [events] to [output]. Frames whose bytes can no
     * longer be read from [readFrame] are skipped, along with anything before
     * the first frame that could be written - the wire format promises the
     * first event after the header is a frame.
     *
     * @return how many event lines were written and how many of them were frames.
     */
    fun encode(
        header: ReplayHeader,
        events: List<ReplayTimedEvent>,
        readFrame: (ReplayFrameRef) -> ByteArray?,
        output: ReplayRecordingOutput,
    ): EncodeResult {
        output.write(line(headerLine(header)))

        var written = 0
        var frames = 0
        var lastT = 0L
        for (timed in events) {
            val event = timed.event
            val jpeg = if (event is ReplayEvent.Frame) readFrame(event.ref) ?: continue else null
            if (frames == 0 && event !is ReplayEvent.Frame) continue

            output.write(line(eventLine(timed.t, event, jpeg)))
            written += 1
            lastT = timed.t
            if (event is ReplayEvent.Frame) frames += 1
        }
        return EncodeResult(eventCount = written, frameCount = frames, lastT = lastT)
    }

    data class EncodeResult(
        val eventCount: Int,
        val frameCount: Int,
        val lastT: Long,
    )

    private fun line(text: String): ByteArray = (text + "\n").encodeToByteArray()

    /** Mirrors the snake_case device object the posts session is minted with. */
    private fun deviceObject(device: SdkPostsSessionDeviceDto): JsonObject = buildJsonObject {
        put("platform", device.platform)
        device.osVersion?.let { put("os_version", it) }
        device.appVersion?.let { put("app_version", it) }
        device.deviceModel?.let { put("device_model", it) }
        device.locale?.let { put("locale", it) }
    }

    /** An integral value as a JSON integer (`3`, not `3.0`), so coordinates and `scale` match iOS exactly. */
    private fun numberValue(value: Double): JsonPrimitive {
        val rounded = round(value)
        return if (value == rounded && rounded >= Long.MIN_VALUE.toDouble() && rounded <= Long.MAX_VALUE.toDouble()) {
            JsonPrimitive(rounded.toLong())
        } else {
            JsonPrimitive(value)
        }
    }
}

/**
 * ISO 8601 UTC with exactly three fractional digits
 * (`2026-09-22T10:00:00.000Z`), as the wire format requires for `start`.
 * Hand-rolled because `kotlin.time.Instant.toString()` drops a zero fraction
 * and the SDK has no kotlinx-datetime dependency to format with.
 */
internal fun formatReplayStart(epochMillis: Long): String {
    val days = epochMillis.floorDiv(MILLIS_PER_DAY)
    val millisOfDay = epochMillis.mod(MILLIS_PER_DAY)

    // Howard Hinnant's civil_from_days.
    val z = days + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = yoe + era * 400 + if (month <= 2) 1 else 0

    val hour = millisOfDay / 3_600_000
    val minute = (millisOfDay / 60_000) % 60
    val second = (millisOfDay / 1_000) % 60
    val millis = millisOfDay % 1_000

    return buildString {
        append(year.toString().padStart(4, '0')).append('-')
        append(month.toString().padStart(2, '0')).append('-')
        append(day.toString().padStart(2, '0')).append('T')
        append(hour.toString().padStart(2, '0')).append(':')
        append(minute.toString().padStart(2, '0')).append(':')
        append(second.toString().padStart(2, '0')).append('.')
        append(millis.toString().padStart(3, '0')).append('Z')
    }
}

private const val MILLIS_PER_DAY = 86_400_000L
