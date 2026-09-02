package com.flabbergast.wandkit.core.components.formPage.mapper

import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import com.flabbergast.wandkit.core.domain.forms.models.PageInput
import kotlin.test.Test
import kotlin.test.assertEquals

class FormPageViewStateMapperTest {
    private fun page(
        content: FeedbackFormPage.Content,
        isRequired: Boolean = false,
        nextButtonLabel: String? = null,
        skipButtonLabel: String? = "Skip",
    ) = FeedbackFormPage(
        id = "page-1",
        title = "title",
        subtitle = null,
        imageUrl = null,
        nextButtonLabel = nextButtonLabel,
        skipButtonLabel = skipButtonLabel,
        promoLabel = null,
        isRequired = isRequired,
        next = emptyList(),
        content = content,
    )

    private fun buttons(page: FeedbackFormPage) =
        formPageViewStateMapper(PageInput(), page)?.buttons.orEmpty()

    private fun secondaryButton(page: FeedbackFormPage) =
        buttons(page).single { it.type == FormPageButton.Type.SECONDARY }

    @Test
    fun textPageDefaultsToContinueWhenNextLabelIsMissing() {
        val mapped = buttons(page(FeedbackFormPage.Content.Text(placeholder = "", maxLength = 100)))

        assertEquals(
            listOf("Continue", "Skip"),
            mapped.map { it.label },
        )
        assertEquals(FormPageButton.Type.PRIMARY, mapped.first().type)
        assertEquals(FormPageButton.Type.SECONDARY, mapped.last().type)
    }

    @Test
    fun multiSelectDefaultsToContinueWhenNextLabelIsMissing() {
        val mapped = buttons(
            page(
                FeedbackFormPage.Content.MultiChoice(
                    options = emptyList(),
                    allowMultiple = true,
                ),
            ),
        )

        assertEquals("Continue", mapped.first().label)
        assertEquals(FormPageButton.Type.PRIMARY, mapped.first().type)
    }

    @Test
    fun singleChoiceWithoutNextLabelHasNoPrimaryButton() {
        val mapped = buttons(
            page(
                FeedbackFormPage.Content.MultiChoice(
                    options = emptyList(),
                    allowMultiple = false,
                ),
                skipButtonLabel = null,
            ),
        )

        assertEquals(emptyList(), mapped)
    }

    @Test
    fun usesServerNextButtonLabelWhenProvided() {
        val mapped = buttons(
            page(
                FeedbackFormPage.Content.Text(placeholder = "", maxLength = 100),
                nextButtonLabel = "Send feedback",
            ),
        )

        assertEquals("Send feedback", mapped.first().label)
    }

    /**
     * A `display_name` page exists to get consent for the name - its Skip
     * button must be a real skip, not a disguised Continue that would
     * silently confirm whatever's currently in the field (e.g. an untouched
     * `suggested_display_name` prefill).
     */
    @Test
    fun displayNamePageSecondaryButtonUsesARealSkipAction() {
        val button = secondaryButton(page(FeedbackFormPage.Content.DisplayName(suggestedName = "Alex")))

        assertEquals(FormPageButton.Action.SKIP, button.action)
    }

    /**
     * Every other page type keeps its existing (if slightly odd) behavior
     * of routing the secondary button through CONTINUE - untouched by the
     * `display_name` fix.
     */
    @Test
    fun otherPageTypesSecondaryButtonKeepsTheExistingContinueAction() {
        val textButton = secondaryButton(page(FeedbackFormPage.Content.Text(placeholder = "", maxLength = 100)))
        val thumbsButton = secondaryButton(page(FeedbackFormPage.Content.Thumbs))
        val starsButton = secondaryButton(page(FeedbackFormPage.Content.Stars(starCount = 5)))
        val multiChoiceButton = secondaryButton(
            page(FeedbackFormPage.Content.MultiChoice(options = emptyList(), allowMultiple = false))
        )

        assertEquals(FormPageButton.Action.CONTINUE, textButton.action)
        assertEquals(FormPageButton.Action.CONTINUE, thumbsButton.action)
        assertEquals(FormPageButton.Action.CONTINUE, starsButton.action)
        assertEquals(FormPageButton.Action.CONTINUE, multiChoiceButton.action)
    }
}
