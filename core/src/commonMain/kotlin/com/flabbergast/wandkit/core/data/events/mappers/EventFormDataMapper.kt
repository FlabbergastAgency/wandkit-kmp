package com.flabbergast.wandkit.core.data.events.mappers

import com.flabbergast.wandkit.core.data.events.dto.EventFormDto
import com.flabbergast.wandkit.core.data.events.dto.EventNextRuleDto
import com.flabbergast.wandkit.core.data.events.dto.EventPageDto
import com.flabbergast.wandkit.core.data.events.dto.EventPageTypeDto
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage

internal fun toFeedbackForm(dto: EventFormDto): FeedbackForm? = FeedbackForm(
    formId = dto.publicId,
    impressionId = dto.impressionId,
    entryPage = dto.pages.firstOrNull()?.mapFormPage() ?: return null,
    pages = dto.pages.associate { page -> page.id to page.mapFormPage() },
    description = dto.description,
)

private fun EventPageDto.mapFormPage(): FeedbackFormPage {
    return FeedbackFormPage(
        id = id,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        nextButtonLabel = nextButtonLabel,
        skipButtonLabel = "Skip", // todo matko: put backend value
        promoLabel = "Powered by WandKit", // todo matko: put backend value
        isRequired = required,
        content = mapContent(),
        next = next.map(::mapNextPageRule)
    )
}

private fun EventPageDto.mapContent() = when (type) {
    EventPageTypeDto.THUMBS -> FeedbackFormPage.Content.Thumbs
    EventPageTypeDto.STARS -> FeedbackFormPage.Content.Stars(
        starCount = 5,
    )
    EventPageTypeDto.MULTI_CHOICE -> FeedbackFormPage.Content.MultiChoice(
        options = options?.map { FeedbackFormPage.Content.MultiChoice.Option(it.id, it.label)} ?: listOf(),
        allowMultiple = allowMultiple ?: false,
    )
    EventPageTypeDto.TEXT -> FeedbackFormPage.Content.Text(
        placeholder = placeholder.orEmpty(),
        maxLength = maxLength ?: Int.MAX_VALUE,
    )
    EventPageTypeDto.END -> FeedbackFormPage.Content.End
}

private fun mapNextPageRule(dto: EventNextRuleDto): FeedbackFormPage.NextPageRule = when {
    dto.condition == null -> FeedbackFormPage.NextPageRule.None(dto.pageId)
    dto.condition == "thumb.up" -> FeedbackFormPage.NextPageRule.Thumbs(nextPageId = dto.pageId, isThumbsUp = true)
    dto.condition == "thumb.down" -> FeedbackFormPage.NextPageRule.Thumbs(nextPageId = dto.pageId, isThumbsUp = false)
    dto.condition.startsWith("star.") -> FeedbackFormPage.NextPageRule.Stars(
        nextPageId = dto.pageId,
        starRating = dto.condition.removePrefix("star.").toIntOrNull() ?: 0
    )
    dto.condition.startsWith("option.") -> FeedbackFormPage.NextPageRule.Option(
        nextPageId = dto.pageId,
        optionId = dto.condition.removePrefix("option.")
    )
    else -> FeedbackFormPage.NextPageRule.None(dto.pageId)
}