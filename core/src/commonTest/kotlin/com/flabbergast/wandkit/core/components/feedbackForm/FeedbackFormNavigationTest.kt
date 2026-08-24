package com.flabbergast.wandkit.core.components.feedbackForm

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import com.flabbergast.wandkit.core.domain.forms.models.PageInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [resolveVisiblePageId] is the client-side safety net for the server's
 * `display_name` splicing rule: skip the page (as if it weren't there) when
 * the form's `post_creation` page answer is empty at navigation time.
 */
class FeedbackFormNavigationTest {
    private fun page(
        id: String,
        content: FeedbackFormPage.Content,
        hasPostCreation: Boolean = false,
        next: List<FeedbackFormPage.NextPageRule> = emptyList(),
    ) = FeedbackFormPage(
        id = id,
        title = "title",
        subtitle = null,
        imageUrl = null,
        nextButtonLabel = null,
        skipButtonLabel = "Skip",
        promoLabel = null,
        isRequired = false,
        next = next,
        content = content,
        hasPostCreation = hasPostCreation,
    )

    private fun form(vararg pages: FeedbackFormPage) = FeedbackForm(
        formId = "form-1",
        impressionId = "impression-1",
        entryPage = pages.first(),
        pages = pages.associateBy { it.id },
        description = null,
    )

    @Test
    fun nonDisplayNamePageIsAlwaysVisible() {
        val testForm = form(
            page(id = "post-creation", content = FeedbackFormPage.Content.Text("", 100), hasPostCreation = true),
            page(id = "text", content = FeedbackFormPage.Content.Text("", 100)),
        )

        val result = resolveVisiblePageId(testForm, pageResults = emptyMap(), candidatePageId = "text")

        assertEquals("text", result)
    }

    @Test
    fun displayNamePageIsVisibleWhenPostCreationAnswered() {
        val testForm = form(
            page(id = "post-creation", content = FeedbackFormPage.Content.Text("", 100), hasPostCreation = true),
            page(id = "display-name", content = FeedbackFormPage.Content.DisplayName(suggestedName = null)),
        )
        val pageResults = mapOf("post-creation" to PageInput(text = "I want a widget"))

        val result = resolveVisiblePageId(testForm, pageResults, candidatePageId = "display-name")

        assertEquals("display-name", result)
    }

    @Test
    fun displayNamePageIsSkippedWhenPostCreationPageWasSkipped() {
        val testForm = form(
            page(id = "post-creation", content = FeedbackFormPage.Content.Text("", 100), hasPostCreation = true),
            page(
                id = "display-name",
                content = FeedbackFormPage.Content.DisplayName(suggestedName = null),
                next = listOf(FeedbackFormPage.NextPageRule.None(nextPageId = "end")),
            ),
            page(id = "end", content = FeedbackFormPage.Content.End),
        )
        // post-creation is absent from pageResults entirely, as if skipped.

        val result = resolveVisiblePageId(testForm, pageResults = emptyMap(), candidatePageId = "display-name")

        assertEquals("end", result)
    }

    @Test
    fun displayNamePageIsSkippedWhenPostCreationAnswerIsPresentButEmpty() {
        val testForm = form(
            page(id = "post-creation", content = FeedbackFormPage.Content.Text("", 100), hasPostCreation = true),
            page(
                id = "display-name",
                content = FeedbackFormPage.Content.DisplayName(suggestedName = null),
                next = listOf(FeedbackFormPage.NextPageRule.None(nextPageId = "end")),
            ),
            page(id = "end", content = FeedbackFormPage.Content.End),
        )
        val pageResults = mapOf("post-creation" to PageInput())

        val result = resolveVisiblePageId(testForm, pageResults, candidatePageId = "display-name")

        assertEquals("end", result)
    }

    @Test
    fun formWithoutAPostCreationPageTreatsDisplayNameAsUnanswered() {
        val testForm = form(
            page(
                id = "display-name",
                content = FeedbackFormPage.Content.DisplayName(suggestedName = null),
                next = listOf(FeedbackFormPage.NextPageRule.None(nextPageId = "end")),
            ),
            page(id = "end", content = FeedbackFormPage.Content.End),
        )

        val result = resolveVisiblePageId(testForm, pageResults = emptyMap(), candidatePageId = "display-name")

        assertEquals("end", result)
    }

    @Test
    fun displayNamePageWithNoNextRuleAndUnansweredPostCreationEndsNavigation() {
        val testForm = form(
            page(id = "post-creation", content = FeedbackFormPage.Content.Text("", 100), hasPostCreation = true),
            page(id = "display-name", content = FeedbackFormPage.Content.DisplayName(suggestedName = null)),
        )

        val result = resolveVisiblePageId(testForm, pageResults = emptyMap(), candidatePageId = "display-name")

        assertNull(result)
    }

    /**
     * [resolveDisplayNameAfterAdvance] backs the fix for a `display_name`
     * page's Skip button: a `display_name` page exists to get consent for
     * the name, so skipping it must never silently confirm a prefilled
     * `suggested_display_name` the user didn't actually type or approve.
     */
    @Test
    fun skippingADisplayNamePageClearsTheConfirmedNameEvenWithAPrefilledSuggestion() {
        val result = resolveDisplayNameAfterAdvance(
            currentDisplayName = null,
            pageContent = FeedbackFormPage.Content.DisplayName(suggestedName = "Alex"),
            action = PageAdvanceAction.SKIP,
            rawText = "Alex", // still holds the untouched prefill in the field
        )

        assertNull(result)
    }

    @Test
    fun continuingADisplayNamePageWithAnUntouchedPrefillConfirmsIt() {
        val result = resolveDisplayNameAfterAdvance(
            currentDisplayName = null,
            pageContent = FeedbackFormPage.Content.DisplayName(suggestedName = "Alex"),
            action = PageAdvanceAction.CONTINUE,
            rawText = "Alex",
        )

        assertEquals("Alex", result)
    }

    @Test
    fun continuingADisplayNamePageAfterClearingTheFieldOmitsTheName() {
        val result = resolveDisplayNameAfterAdvance(
            currentDisplayName = null,
            pageContent = FeedbackFormPage.Content.DisplayName(suggestedName = "Alex"),
            action = PageAdvanceAction.CONTINUE,
            rawText = "",
        )

        assertNull(result)
    }

    @Test
    fun continuingADisplayNamePageTrimsWhitespaceOnlyTextToNull() {
        val result = resolveDisplayNameAfterAdvance(
            currentDisplayName = null,
            pageContent = FeedbackFormPage.Content.DisplayName(suggestedName = null),
            action = PageAdvanceAction.CONTINUE,
            rawText = "   ",
        )

        assertNull(result)
    }

    @Test
    fun advancingPastANonDisplayNamePageNeverTouchesTheConfirmedName() {
        val skipResult = resolveDisplayNameAfterAdvance(
            currentDisplayName = "Alex",
            pageContent = FeedbackFormPage.Content.Text("", 100),
            action = PageAdvanceAction.SKIP,
            rawText = null,
        )
        val continueResult = resolveDisplayNameAfterAdvance(
            currentDisplayName = "Alex",
            pageContent = FeedbackFormPage.Content.Text("", 100),
            action = PageAdvanceAction.CONTINUE,
            rawText = "unrelated text page input",
        )

        assertEquals("Alex", skipResult)
        assertEquals("Alex", continueResult)
    }
}
