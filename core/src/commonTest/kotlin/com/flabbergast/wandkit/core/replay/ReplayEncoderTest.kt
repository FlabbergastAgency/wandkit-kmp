package com.flabbergast.wandkit.core.replay

import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplayEncoderTest {
    private val fixtureLines = REPLAY_FIXTURE_V1.lines()

    /** 2026-09-22T10:00:00.000Z, the fixture header's `start`. */
    private val fixtureStart = 1_790_071_200_000L

    @Test
    fun encoderReproducesTheSharedFixture() {
        val parsed = fixtureLines.map { Json.parseToJsonElement(it).jsonObject }
        val headerJson = parsed.first()
        val device = headerJson.getValue("device").jsonObject

        val header = ReplayHeader(
            startEpochMillis = fixtureStart,
            width = headerJson.getValue("width").jsonPrimitive.int,
            height = headerJson.getValue("height").jsonPrimitive.int,
            scale = headerJson.getValue("scale").jsonPrimitive.double,
            device = SdkPostsSessionDeviceDto(
                platform = device.getValue("platform").jsonPrimitive.content,
                osVersion = device.getValue("os_version").jsonPrimitive.content,
                appVersion = device.getValue("app_version").jsonPrimitive.content,
                deviceModel = device.getValue("device_model").jsonPrimitive.content,
                locale = device.getValue("locale").jsonPrimitive.content,
            ),
            sdk = headerJson.getValue("sdk").jsonPrimitive.content,
        )

        val store = InMemoryReplayFrameStore()
        val events = parsed.drop(1).map { line -> ReplayTimedEvent(line.getValue("t").jsonPrimitive.long, line.toEvent(store)) }

        val output = InMemoryRecordingOutput()
        val result = ReplayEncoder.encode(header, events, store::read, output)
        val encoded = output.finish().readBytes()!!.decodeToString()

        assertTrue(encoded.endsWith("\n"), "Every line, the last included, is newline-terminated")
        val encodedLines = encoded.trimEnd('\n').split('\n')
        assertEquals(fixtureLines.size, encodedLines.size)
        encodedLines.zip(fixtureLines).forEachIndexed { index, (actual, expected) ->
            // Key order is irrelevant to readers; JsonObject equality ignores it.
            assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual), "Line $index differs")
        }
        assertEquals(fixtureLines.size - 1, result.eventCount)
        assertEquals(3, result.frameCount)
        assertEquals(46_000L, result.lastT)
    }

    @Test
    fun integralNumbersAreWrittenAsIntegers() {
        val line = ReplayEncoder.eventLine(5, ReplayEvent.Touch(ReplayTouchPhase.MOVE, 124.0, 430.5))

        assertEquals("""{"t":5,"e":"touch","p":"move","x":124,"y":430.5}""", line)
    }

    @Test
    fun eventWithoutDataOmitsTheKeyAndEscapesItsName() {
        val line = ReplayEncoder.eventLine(0, ReplayEvent.Event("say \"hi\"", null))

        assertEquals("""{"t":0,"e":"event","name":"say \"hi\""}""", line)
    }

    @Test
    fun framesThatCanNoLongerBeReadAreSkippedWithAnythingBeforeTheFirstWrittenFrame() {
        val store = TrackingFrameStore()
        val lost = store.frame(4)
        val kept = store.frame(4)
        store.delete(lost.ref)

        val output = InMemoryRecordingOutput()
        val result = ReplayEncoder.encode(
            header = ReplayHeader(0, 1, 1, 1.0, SdkPostsSessionDeviceDto(platform = "android"), "android-test"),
            events = listOf(
                ReplayTimedEvent(0, lost),
                ReplayTimedEvent(10, ReplayEvent.Touch(ReplayTouchPhase.DOWN, 0.0, 0.0)),
                ReplayTimedEvent(20, kept),
                ReplayTimedEvent(30, ReplayEvent.Gap(5)),
            ),
            readFrame = store::read,
            output = output,
        )

        assertEquals(2, result.eventCount)
        assertEquals(1, result.frameCount)
        val lines = output.finish().readBytes()!!.decodeToString().trimEnd('\n').split('\n')
        assertTrue(lines[1].startsWith("""{"t":20,"e":"frame""""), lines[1])
    }

    @Test
    fun eventPropertiesBecomeStringDataAndNoPropertiesMeansNoData() {
        assertEquals(null, ReplayEvent.Event.fromProperties("tap", emptyMap()).data)
        assertEquals(
            """{"t":1,"e":"event","name":"tap","data":{"screen":"Settings"}}""",
            ReplayEncoder.eventLine(1, ReplayEvent.Event.fromProperties("tap", mapOf("screen" to "Settings"))),
        )
    }

    @Test
    fun startIsIso8601UtcWithMilliseconds() {
        assertEquals("1970-01-01T00:00:00.000Z", formatReplayStart(0))
        assertEquals("2026-09-22T10:00:00.000Z", formatReplayStart(fixtureStart))
        assertEquals("2024-02-29T23:59:59.999Z", formatReplayStart(1_709_251_199_999L))
        assertEquals("1969-12-31T23:59:59.999Z", formatReplayStart(-1))
    }

    private fun JsonObject.toEvent(store: ReplayFrameStore): ReplayEvent {
        val primitive = { key: String -> getValue(key).jsonPrimitive }
        return when (val kind = primitive("e").content) {
            "frame" -> ReplayEvent.Frame(
                width = primitive("w").int,
                height = primitive("h").int,
                ref = store.put(Base64.decode(primitive("jpeg").content))!!,
            )
            "touch" -> ReplayEvent.Touch(
                phase = ReplayTouchPhase.entries.first { it.wireValue == primitive("p").content },
                x = primitive("x").double,
                y = primitive("y").double,
            )
            "event" -> ReplayEvent.Event(
                name = primitive("name").content,
                data = this["data"]?.jsonObject,
            )
            "gap" -> ReplayEvent.Gap(primitive("ms").long)
            else -> error("Unexpected event kind $kind")
        }
    }
}
