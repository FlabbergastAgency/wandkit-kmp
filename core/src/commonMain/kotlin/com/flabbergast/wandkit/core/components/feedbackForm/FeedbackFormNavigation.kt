package com.flabbergast.wandkit.core.components.feedbackForm

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.domain.forms.models.PageInput

/**
 * Pure navigation-resolution helpers for [DefaultFeedbackFormComponent],
 * split out so the branching logic (in particular the `display_name`
 * visibility rule below) is unit-testable without decompose scaffolding.
 */

/** Applies [current]'s `next` rules against [result], first match wins. */
internal fun resolveNextPageId(
    current: FeedbackFormPage,
    result: PageInput?,
): FeedbackFormPageId? =
    current.next.firstNotNullOfOrNull { rule ->
        when (rule) {
            is FeedbackFormPage.NextPageRule.None -> rule.nextPageId
            is FeedbackFormPage.NextPageRule.Option -> rule.takeIf { result?.optionIds?.contains(it.optionId) ?: false }?.nextPageId
            is FeedbackFormPage.NextPageRule.Stars -> rule.takeIf { result?.stars == it.starRating }?.nextPageId
            is FeedbackFormPage.NextPageRule.Thumbs -> rule.takeIf { result?.isThumbsUp == it.isThumbsUp }?.nextPageId
        }
    }

/**
 * True when the form's `post_creation` page (see
 * [FeedbackFormPage.hasPostCreation]) has a non-empty recorded answer. Used
 * as the client-side gate for whether a `display_name` page should be shown
 * - mirrors the server's own "answer will create a post" check, as a safety
 * net for whatever path the page reached this client through.
 *
 * A form without a `post_creation` page at all is treated as "not
 * answered": there is nothing a `display_name` page could be attached to.
 */
internal fun isPostCreationAnswered(
    form: FeedbackForm,
    pageResults: Map<FeedbackFormPageId, PageInput>,
): Boolean {
    val postCreationPageId = form.pages.values.firstOrNull { it.hasPostCreation }?.id ?: return false
    return pageResults[postCreationPageId]?.hasInput() == true
}

/**
 * Resolves [candidatePageId] to the next page that should actually be shown,
 * transparently skipping over one or more `display_name` pages (as if they
 * were absent, following their own `next` rules with no result - the same
 * shape as a manually skipped page) when [isPostCreationAnswered] is false.
 *
 * Returns null when skipping runs off the end of the graph with no further
 * page to show, meaning the caller should submit/finish the form instead.
 * Returns [candidatePageId] unchanged when it isn't backed by a known page
 * (defensive fallback - let the caller navigate to it and fail there).
 */
internal tailrec fun resolveVisiblePageId(
    form: FeedbackForm,
    pageResults: Map<FeedbackFormPageId, PageInput>,
    candidatePageId: FeedbackFormPageId,
): FeedbackFormPageId? {
    val candidate = form.pages[candidatePageId] ?: return candidatePageId
    if (candidate.content !is FeedbackFormPage.Content.DisplayName || isPostCreationAnswered(form, pageResults)) {
        return candidatePageId
    }
    val skippedNext = resolveNextPageId(candidate, null) ?: return null
    return resolveVisiblePageId(form, pageResults, skippedNext)
}

/** Distinguishes a genuine confirmation (primary button) from an explicit skip (secondary button) when advancing past a page. */
internal enum class PageAdvanceAction { CONTINUE, SKIP }

/**
 * Resolves what the form's confirmed `display_name` state should become
 * after advancing past a page whose content is [pageContent].
 *
 * Only a [PageAdvanceAction.CONTINUE] tap on an actual `display_name` page
 * commits [rawText] - trimmed, with blank collapsed to null so it is omitted
 * from the submit request rather than sent blank. A
 * [PageAdvanceAction.SKIP] always clears it to null, even when [rawText]
 * still carries a prefilled `suggested_display_name` the user never
 * explicitly confirmed - the page exists to get consent for the name, so a
 * skip must never silently confirm a suggestion. Advancing past any other
 * page type leaves [currentDisplayName] untouched, since `display_name` is
 * a single page's concern.
 */
internal fun resolveDisplayNameAfterAdvance(
    currentDisplayName: String?,
    pageContent: FeedbackFormPage.Content,
    action: PageAdvanceAction,
    rawText: String?,
): String? = when {
    pageContent !is FeedbackFormPage.Content.DisplayName -> currentDisplayName
    action == PageAdvanceAction.SKIP -> null
    else -> rawText?.trim()?.takeIf(String::isNotEmpty)
}
