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

            is FeedbackFormPage.Content.DisplayName -> FormPageUiState.Content.DisplayName(
                name = input.text ?: content.suggestedName.orEmpty(),
            )

            is FeedbackFormPage.Content.Thumbs -> FormPageUiState.Content.Thumbs(
                isThumbsUp = input.isThumbsUp,
            )
        },
        buttons = mapButtons(page),
        promoLabel = page.promoLabel,
    )
}

/** Default primary-button label for content types with no server-set copy of their own. */
private const val DEFAULT_CONTINUE_BUTTON_LABEL = "Continue"

private fun mapButtons(page: FeedbackFormPage?) = buildList {
    if (page?.content is FeedbackFormPage.Content.End) return@buildList

    // `display_name` pages always show a primary button - "Continue" unless
    // the server overrides it - since (unlike thumbs/stars) picking a value
    // doesn't auto-advance the page.
    val primaryButtonLabel = page?.nextButtonLabel
        ?: DEFAULT_CONTINUE_BUTTON_LABEL.takeIf { page?.content is FeedbackFormPage.Content.DisplayName }

    primaryButtonLabel?.let { label ->
        add(
            FormPageButton(
                label = label,
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
                // Every other page type's secondary button intentionally
                // still routes through CONTINUE (an existing, unrelated
                // quirk left as-is). `display_name` is the one page that
                // must not silently confirm a prefilled suggestion when the
                // user taps "Skip", so it alone gets a real SKIP action.
                action = if (page.content is FeedbackFormPage.Content.DisplayName) {
                    FormPageButton.Action.SKIP
                } else {
                    FormPageButton.Action.CONTINUE
                },
            )
        )
    }
}
