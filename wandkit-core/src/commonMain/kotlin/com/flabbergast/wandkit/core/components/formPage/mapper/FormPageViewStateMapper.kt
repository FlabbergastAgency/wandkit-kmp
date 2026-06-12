package com.flabbergast.wandkit.core.components.formPage.mapper

import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import com.flabbergast.wandkit.core.domain.forms.models.PageInput

internal fun formPageViewStateMapper(
    input: PageInput,
    page: FeedbackFormPage?,
) = page?.let { page ->
    FormPageUiState(
        id = page.id,
        title = page.title,
        subtitle = page.subtitle,
        imageUrl = page.imageUrl,
        content = when (val content = page.content) {
            is FeedbackFormPage.Content.End -> FormPageUiState.Content.End
            is FeedbackFormPage.Content.MultiChoice -> FormPageUiState.Content.MultiChoice(
                choices = content.options.map {
                    FormPageUiState.Content.MultiChoice.Option(
                        id = it.id,
                        label = it.label,
                        isSelected = input.optionIds?.contains(it.id) ?: false,
                    )
                },
                allowMultiple = content.allowMultiple,
            )

            is FeedbackFormPage.Content.Stars -> FormPageUiState.Content.Stars(
                totalStars = content.starCount,
                selectedStars = input.stars,
            )

            is FeedbackFormPage.Content.Text -> FormPageUiState.Content.Text(
                placeholder = content.placeholder,
                maxLength = content.maxLength,
                text = input.text.orEmpty(),
            )

            is FeedbackFormPage.Content.Thumbs -> FormPageUiState.Content.Thumbs(
                isThumbsUp = input.isThumbsUp,
            )
        },
        buttons = mapButtons(page),
        promoLabel = page.promoLabel,
    )
}

private fun mapButtons(page: FeedbackFormPage?) = buildList {
    if (page?.content is FeedbackFormPage.Content.End) return@buildList

    page?.nextButtonLabel?.let { nextButtonLabel ->
        add(
            FormPageButton(
                label = nextButtonLabel,
                type = FormPageButton.Type.PRIMARY,
                action = FormPageButton.Action.CONTINUE,
            )
        )
    }

    if (page?.isRequired == false && page.skipButtonLabel != null) {
        add(
            FormPageButton(
                label = page.skipButtonLabel,
                type = FormPageButton.Type.SECONDARY,
                action = FormPageButton.Action.CONTINUE,
            )
        )
    }
}
