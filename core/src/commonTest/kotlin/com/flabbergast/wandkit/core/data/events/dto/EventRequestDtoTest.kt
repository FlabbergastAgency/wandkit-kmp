package com.flabbergast.wandkit.core.data.events.dto

import com.flabbergast.wandkit.core.data.networking.createJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventRequestDtoTest {
    @Test
    fun deviceEncodesAllFieldsAsSnakeCase() {
        val json = createJson()
        val request = EventRequestDto(
            eventName = "screen_viewed",
            user = EventRequestUserDto(externalUserId = "user-1", deviceId = "device-1"),
            occurredAt = "2026-08-20T00:00:00Z",
            sdk = EventRequestSdkDto(platform = "Android", version = "1.0.0"),
            device = EventRequestDeviceDto(
                platform = "android",
                osVersion = "14",
                appVersion = "1.2.3",
                deviceModel = "Pixel 8",
                locale = "en-US",
            ),
        )

        val encoded = json.encodeToString(EventRequestDto.serializer(), request)

        assertTrue(encoded.contains("\"platform\":\"android\""))
        assertTrue(encoded.contains("\"os_version\":\"14\""))
        assertTrue(encoded.contains("\"app_version\":\"1.2.3\""))
        assertTrue(encoded.contains("\"device_model\":\"Pixel 8\""))
        assertTrue(encoded.contains("\"locale\":\"en-US\""))
    }

    @Test
    fun absentDeviceIsOmittedFromRequestBody() {
        val json = createJson()
        val request = EventRequestDto(
            eventName = "screen_viewed",
            user = EventRequestUserDto(externalUserId = "user-1", deviceId = "device-1"),
            occurredAt = "2026-08-20T00:00:00Z",
            sdk = EventRequestSdkDto(platform = "Android", version = "1.0.0"),
        )

        val encoded = json.encodeToString(EventRequestDto.serializer(), request)

        assertFalse(encoded.contains("\"device\""))
    }

    @Test
    fun nullDeviceFieldsAreOmittedNotSentAsNull() {
        val json = createJson()
        val device = EventRequestDeviceDto(
            platform = "ios",
            osVersion = null,
            appVersion = null,
            deviceModel = null,
            locale = null,
        )

        val encoded = json.encodeToString(EventRequestDeviceDto.serializer(), device)

        assertEquals("""{"platform":"ios"}""", encoded)
    }
}
