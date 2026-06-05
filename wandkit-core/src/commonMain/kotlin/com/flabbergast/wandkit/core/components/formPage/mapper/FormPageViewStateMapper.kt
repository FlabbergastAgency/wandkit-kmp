package com.flabbergast.wandkit.core.components.formPage.mapper

import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import com.flabbergast.wandkit.core.components.formPage.model.PageInput

internal fun formPageViewStateMapper(
    input: PageInput,
    page: FeedbackFormPage?,
) = page?.let { page ->
    FormPageUiState(
        id = page.id,
        title = page.title,
        subtitle = page.subtitle,
        imageUrl = page.imageUrl,
        nextButtonLabel = page.nextButtonLabel,
        content = when (page) {
            is FeedbackFormPage.End -> FormPageUiState.Content.End
            is FeedbackFormPage.MultiChoice -> FormPageUiState.Content.MultiChoice(
                choices = page.options.map {
                    FormPageUiState.Content.MultiChoice.Option(
                        id = it.id,
                        label = it.label,
                        isSelected = input.optionIds?.contains(it.id) ?: false,
                    )
                },
                allowMultiple = page.allowMultiple,
            )

            is FeedbackFormPage.Stars -> FormPageUiState.Content.Stars(
                totalStars = page.starCount,
                selectedStars = input.stars,
            )

            is FeedbackFormPage.Text -> FormPageUiState.Content.Text(
                placeholder = page.placeholder,
                maxLength = page.maxLength,
                text = input.text.orEmpty(),
            )

            is FeedbackFormPage.Thumbs -> FormPageUiState.Content.Thumbs(
                isThumbsUp = input.isThumbsUp,
            )
        },
    )
}
