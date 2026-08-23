package com.flabbergast.wandkit.core.data.events.dto

import com.flabbergast.wandkit.core.data.networking.createJson
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The backend can now attach a `post_creation` object to text pages
 * (`{ "default_type": "bug" }`) that this SDK does not model or act on - KMP
 * has no posts UI yet. Confirms the shared `Json` config (`ignoreUnknownKeys`)
 * lets old and new SDKs alike decode a page carrying that field without
 * blowing up, matching how this DTO already drops other fields it does not
 * use (e.g. the iOS-only `action`).
 */
class EventPageDtoTest {
    @Test
    fun decodesPageWithUnknownPostCreationField() {
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
    }
}
