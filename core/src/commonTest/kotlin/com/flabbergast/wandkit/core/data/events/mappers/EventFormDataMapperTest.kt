package com.flabbergast.wandkit.core.data.events.mappers

import com.flabbergast.wandkit.core.data.events.dto.EventFormDto
import com.flabbergast.wandkit.core.data.events.dto.EventPageDto
import com.flabbergast.wandkit.core.data.events.dto.EventPageTypeDto
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventFormDataMapperTest {
    private fun formDto(vararg pages: EventPageDto) = EventFormDto(
        publicId = "form-1",
        impressionId = "impression-1",
        title = "Feedback",
        description = null,
        pages = pages.toList(),
    )

    @Test
    fun mapsDisplayNamePageTypeWithSuggestedName() {
        val form = toFeedbackForm(
            formDto(
                EventPageDto(
                    id = "page-name",
                    type = EventPageTypeDto.DISPLAY_NAME,
                    title = "What should we call you?",
                    suggestedDisplayName = "Alex",
                ),
            )
        )

        val content = form?.pages?.get("page-name")?.content as? FeedbackFormPage.Content.DisplayName
        assertEquals("Alex", content?.suggestedName)
    }

    @Test
    fun mapsDisplayNamePageTypeWithoutSuggestedName() {
        val form = toFeedbackForm(
            formDto(
                EventPageDto(
                    id = "page-name",
                    type = EventPageTypeDto.DISPLAY_NAME,
                    title = "What should we call you?",
                ),
            )
        )

        val content = form?.pages?.get("page-name")?.content as? FeedbackFormPage.Content.DisplayName
        assertNull(content?.suggestedName)
    }

    @Test
    fun marksPageAsHavingPostCreationWhenFieldPresent() {
        val form = toFeedbackForm(
            formDto(
                EventPageDto(
                    id = "page-text",
                    type = EventPageTypeDto.TEXT,
                    title = "Anything else?",
                    postCreation = JsonObject(emptyMap()),
                ),
            )
        )

        assertTrue(form?.pages?.get("page-text")?.hasPostCreation == true)
    }

    @Test
    fun doesNotMarkPageAsHavingPostCreationWhenFieldAbsent() {
        val form = toFeedbackForm(
            formDto(
                EventPageDto(
                    id = "page-text",
                    type = EventPageTypeDto.TEXT,
                    title = "Anything else?",
                ),
            )
        )

        assertFalse(form?.pages?.get("page-text")?.hasPostCreation == true)
    }
}
