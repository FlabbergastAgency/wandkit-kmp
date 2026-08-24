package com.flabbergast.wandkit.core.data.events.dto

import com.flabbergast.wandkit.core.data.networking.createJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The backend can attach a `post_creation` object to text pages
 * (`{ "default_type": "bug" }`), at most one per form. KMP has no posts UI
 * and doesn't model or act on its contents - it only decodes *whether* the
 * key is present, as a marker consumed by
 * `FeedbackFormPage.hasPostCreation` to gate a later `display_name` page's
 * visibility. Confirms the shared
 * `Json` config (`ignoreUnknownKeys`) still lets old and new SDKs alike
 * decode a page carrying fields this DTO doesn't otherwise use (e.g. the
 * iOS-only `action`).
 */
class EventPageDtoTest {
    @Test
    fun decodesPostCreationAsAPresenceMarker() {
        val json = createJson()

        val page = json.decodeFromString(
            EventPageDto.serializer(),
            """
            {
              "id": "page-1",
              "type": "text",
              "title": "Anything else?",
              "post_creation": {"default_type": "bug"}
            }
            """.trimIndent(),
        )

        assertEquals("page-1", page.id)
        assertEquals("Anything else?", page.title)
        assertTrue(page.postCreation != null)
    }

    @Test
    fun decodesDisplayNamePageWithSuggestedName() {
        val json = createJson()

        val page = json.decodeFromString(
            EventPageDto.serializer(),
            """
            {
              "id": "page-name",
              "type": "display_name",
              "title": "What should we call you?",
              "suggested_display_name": "Alex"
            }
            """.trimIndent(),
        )

        assertEquals(EventPageTypeDto.DISPLAY_NAME, page.type)
        assertEquals("Alex", page.suggestedDisplayName)
    }

    @Test
    fun decodesDisplayNamePageWithoutSuggestedName() {
        val json = createJson()

        val page = json.decodeFromString(
            EventPageDto.serializer(),
            """
            {
              "id": "page-name",
              "type": "display_name",
              "title": "What should we call you?"
            }
            """.trimIndent(),
        )

        assertEquals(EventPageTypeDto.DISPLAY_NAME, page.type)
        assertNull(page.suggestedDisplayName)
    }

    /** Old payloads predate both fields entirely - neither key is sent. */
    @Test
    fun oldPayloadWithoutPostCreationOrSuggestedDisplayNameDecodesWithNullDefaults() {
        val json = createJson()

        val page = json.decodeFromString(
            EventPageDto.serializer(),
            """
            {
              "id": "page-1",
              "type": "text",
              "title": "Anything else?"
            }
            """.trimIndent(),
        )

        assertNull(page.postCreation)
        assertNull(page.suggestedDisplayName)
    }
}
