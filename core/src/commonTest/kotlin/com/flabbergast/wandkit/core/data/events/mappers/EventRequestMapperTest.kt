package com.flabbergast.wandkit.core.data.events.mappers

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.events.dto.EVENT_REQUEST_SUPPORTED_PAGE_TYPES
import com.flabbergast.wandkit.core.data.events.dto.EventRequestDeviceDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestUserDto
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.data.forms.mappers.toSubmitFormDeviceDto
import com.flabbergast.wandkit.core.domain.infrastructure.logger.LogLevel
import com.flabbergast.wandkit.core.platform.DeviceContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventRequestMapperTest {
    @Test
    fun toEventRequestSdkAlwaysReportsSupportedPageTypes() {
        val configuration = AppConfiguration(
            baseUrl = "https://example.test",
            libraryVersion = "1.0.0",
            platformName = "Android",
            platformVersion = "14",
            logLevel = LogLevel.NONE,
        )

        val sdk = configuration.toEventRequestSdk()

        assertEquals(EVENT_REQUEST_SUPPORTED_PAGE_TYPES, sdk.supportedPageTypes)
    }

    private fun configuration(platformName: String) = AppConfiguration(
        baseUrl = "https://example.test",
        libraryVersion = "1.0.0",
        platformName = platformName,
        platformVersion = "14",
        logLevel = LogLevel.NONE,
    )

    private fun deviceContext(appIdentifier: String?) = DeviceContext(
        osVersion = "14",
        appVersion = "1.2.3",
        deviceModel = "Pixel 8",
        locale = "en-US",
        appIdentifier = appIdentifier,
    )

    @Test
    fun sdkPlatformIsTheCanonicalLowercaseValue() {
        assertEquals("android", configuration("Android").toEventRequestSdk().platform)
        assertEquals("ios", configuration("iOS").toEventRequestSdk().platform)
        assertEquals("ios", configuration("iPadOS").toEventRequestSdk().platform)
    }

    @Test
    fun deviceCarriesTheAppIdentifierOnEventsAndFormSubmissions() {
        val config = configuration("Android")
        val context = deviceContext("com.example.app")

        val eventDevice = config.toEventRequestDevice(context)
        val formDevice = config.toSubmitFormDeviceDto(context)

        assertEquals("android", eventDevice.platform)
        assertEquals("com.example.app", eventDevice.appIdentifier)
        assertEquals("android", formDevice.platform)
        assertEquals("com.example.app", formDevice.appIdentifier)

        val encoded = createJson().encodeToString(EventRequestDeviceDto.serializer(), eventDevice)
        assertTrue(encoded.contains("\"app_identifier\":\"com.example.app\""))
    }

    @Test
    fun deviceOmitsTheAppIdentifierWhenUnknown() {
        val device = configuration("iOS").toEventRequestDevice(deviceContext(appIdentifier = null))

        assertNull(device.appIdentifier)
        val encoded = createJson().encodeToString(EventRequestDeviceDto.serializer(), device)
        assertFalse(encoded.contains("app_identifier"))
    }

    @Test
    fun toEventRequestUserIncludesDisplayNameWhenIdentifiedWithAName() {
        val identifyInfo = IdentifyInfo(userId = "user-1", deviceId = "device-1", displayName = "Jane")

        val user = identifyInfo.toEventRequestUser()

        assertEquals("user-1", user.externalUserId)
        assertEquals("Jane", user.displayName)

        val encoded = createJson().encodeToString(EventRequestUserDto.serializer(), user)
        assertTrue(encoded.contains("\"display_name\":\"Jane\""))
    }

    @Test
    fun toEventRequestUserOmitsDisplayNameWhenNoNameWasSuggested() {
        val identifyInfo = IdentifyInfo(userId = "user-1", deviceId = "device-1", displayName = null)

        val user = identifyInfo.toEventRequestUser()

        assertNull(user.displayName)

        val encoded = createJson().encodeToString(EventRequestUserDto.serializer(), user)
        assertFalse(encoded.contains("\"display_name\""))
    }

    /**
     * Mirrors [com.flabbergast.wandkit.core.di.WandKitSdkContainer.identityInfo]: an
     * anonymous install has no user id at all (never a made-up one) and no name, even
     * if the container is still holding a suggestion from an earlier `identify(...)`.
     */
    @Test
    fun toEventRequestUserOmitsUserIdAndDisplayNameForAnonymousEvent() {
        val anonymousIdentifyInfo = IdentifyInfo(
            userId = null,
            deviceId = "device-1",
            displayName = null,
        )

        val user = anonymousIdentifyInfo.toEventRequestUser()

        assertNull(user.externalUserId)
        assertEquals("device-1", user.deviceId)
        assertNull(user.displayName)

        val encoded = createJson().encodeToString(EventRequestUserDto.serializer(), user)
        assertEquals("""{"device_id":"device-1"}""", encoded)
    }

    @Test
    fun toEventRequestUserKeepsTheExternalUserIdWhenIdentified() {
        val user = IdentifyInfo(userId = "user-1", deviceId = "device-1").toEventRequestUser()

        val encoded = createJson().encodeToString(EventRequestUserDto.serializer(), user)
        assertTrue(encoded.contains("\"external_user_id\":\"user-1\""))
        assertTrue(encoded.contains("\"device_id\":\"device-1\""))
    }
}
