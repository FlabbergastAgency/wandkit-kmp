package com.flabbergast.wandkit.core.domain.forms

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun page(
    id: String = "rate",
    content: FeedbackFormPage.Content = FeedbackFormPage.Content.Stars(starCount = 5),
    requestNativeReview: Boolean = true,
) = FeedbackFormPage(
    id = id,
    title = "How was it?",
    subtitle = null,
    imageUrl = null,
    nextButtonLabel = null,
    skipButtonLabel = null,
    promoLabel = null,
    isRequired = false,
    next = emptyList(),
    content = content,
    requestNativeReview = requestNativeReview,
)

private fun form(vararg pages: FeedbackFormPage) = FeedbackForm(
    formId = "form-1",
    impressionId = "impression-1",
    entryPage = pages.first(),
    pages = pages.associateBy { it.id },
    description = null,
)

class NativeReviewRequestTest {
    // region The 5-star rule

    @Test
    fun topRatingOnFlaggedStarsPageRequestsReview() {
        assertTrue(page().requestsNativeReview(stars = 5))
    }

    @Test
    fun lowerRatingsDoNotRequestReview() {
        for (stars in 1..4) {
            assertFalse(page().requestsNativeReview(stars), "$stars stars")
        }
    }

    @Test
    fun unflaggedStarsPageNeverRequestsReview() {
        assertFalse(page(requestNativeReview = false).requestsNativeReview(stars = 5))
    }

    @Test
    fun flagOnAnotherPageTypeIsIgnored() {
        assertFalse(page(content = FeedbackFormPage.Content.Thumbs).requestsNativeReview(stars = 5))
    }

    // endregion

    // region Requesting on close

    private class Harness {
        val calls = mutableListOf<String>()
        val controller = createFeedbackFormController(
            logger = NoOpLogger,
            nativeReviewHooks = object : NativeReviewHooks {
                override fun prefetch() { calls += "prefetch" }
                override fun request() { calls += "request" }
                override fun discard() { calls += "discard" }
            },
        )

        /** Hook calls since the form was published, which itself discards. */
        fun callsAfterPublish() = calls.drop(1)
    }

    @Test
    fun topRatingPrefetchesOnceAndRequestsOnceOnClose() {
        val harness = Harness()
        harness.controller.publish(form(page()))
        harness.controller.markNativeReviewRequested()
        harness.controller.markNativeReviewRequested()

        harness.controller.dismiss()
        harness.controller.dismiss()

        assertEquals(listOf("prefetch", "request"), harness.callsAfterPublish())
    }

    @Test
    fun closingWithoutTopRatingDiscards() {
        val harness = Harness()
        harness.controller.publish(form(page()))

        harness.controller.dismiss()

        assertEquals(listOf("discard"), harness.callsAfterPublish())
    }

    /** Like iOS: once a top rating asked for the review, it stays asked for until the form closes. */
    @Test
    fun theRequestStaysOnceMarked() {
        val harness = Harness()
        harness.controller.publish(form(page()))
        harness.controller.markNativeReviewRequested()

        harness.controller.dismiss()

        assertEquals(listOf("prefetch", "request"), harness.callsAfterPublish())
    }

    @Test
    fun ratingWithNoFormUpIsIgnored() {
        val harness = Harness()
        harness.controller.markNativeReviewRequested()
        harness.controller.publish(form(page()))

        harness.controller.dismiss()

        assertEquals(listOf("discard", "discard"), harness.calls)
    }

    @Test
    fun aNewFormStartsWithoutARequest() {
        val harness = Harness()
        harness.controller.publish(form(page()))
        harness.controller.markNativeReviewRequested()
        harness.controller.dismiss()

        harness.controller.publish(form(page()))
        harness.controller.dismiss()

        assertEquals(listOf("prefetch", "request", "discard", "discard"), harness.callsAfterPublish())
    }

    // endregion
}
