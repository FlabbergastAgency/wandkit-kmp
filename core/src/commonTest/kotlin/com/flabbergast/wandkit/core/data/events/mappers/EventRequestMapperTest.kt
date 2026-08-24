package com.flabbergast.wandkit.core.data.events.mappers

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.events.dto.EVENT_REQUEST_SUPPORTED_PAGE_TYPES
import com.flabbergast.wandkit.core.data.events.dto.EventRequestUserDto
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.domain.infrastructure.logger.LogLevel
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
     * Mirrors [com.flabbergast.wandkit.core.di.WandKitSdkContainer.identityInfo]: it only
     * threads a name through when there is a real identified `externalUserId`, so the random
     * UUID fallback used for anonymous events must reach this mapper with a `null` name even
     * if the container is still holding a suggestion from an earlier `identify(...)` call.
     */
    @Test
    fun toEventRequestUserOmitsDisplayNameForAnonymousEventDespiteLingeringName() {
        val anonymousIdentifyInfo = IdentifyInfo(
            userId = "3f6e9e2a-9b3b-4a34-8e34-2e6b8f6a9c11",
            deviceId = "device-1",
            displayName = null,
        )

        val user = anonymousIdentifyInfo.toEventRequestUser()

        assertNull(user.displayName)

        val encoded = createJson().encodeToString(EventRequestUserDto.serializer(), user)
        assertFalse(encoded.contains("\"display_name\""))
    }
}
