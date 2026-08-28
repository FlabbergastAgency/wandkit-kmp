package com.flabbergast.wandkit.core.domain.forms.models

internal typealias ImpressionId = String

internal data class FeedbackForm(
    val formId: String,
    val impressionId: ImpressionId,
    val entryPage: FeedbackFormPage,
    val pages: Map<FeedbackFormPageId, FeedbackFormPage>,
    val description: String?,
)

internal typealias FeedbackFormPageId = String

internal data class FeedbackFormPage(
    val id: FeedbackFormPageId,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val nextButtonLabel: String?,
    val skipButtonLabel: String?,
    val promoLabel: String?,
    val isRequired: Boolean,
    val next: List<NextPageRule>,
    val content: Content,
    /**
     * True when this is the form's `post_creation` page (the backend sends
     * at most one). Used purely as a client-side visibility gate for a
     * server-spliced [Content.DisplayName] page later in the graph - the
     * SDK never models what `post_creation` itself would do.
     */
    val hasPostCreation: Boolean = false,
) {
    sealed interface Content {
        data object Thumbs: Content

        data class Stars(
            val starCount: Int,
        ): Content

        data class MultiChoice(
            val options: List<Option>,
            val allowMultiple: Boolean,
        ): Content {
            data class Option(
                val id: String,
                val label: String,
            )
        }

        data class Text(
            val placeholder: String,
            val maxLength: Int,
        ): Content

        /**
         * Server-spliced name-confirmation page. Never authored in a form
         * definition; the server inserts it right after the `post_creation`
         * page when the response will create a post and the responder has no
         * display name set yet. Carries no answer - the confirmed name
         * travels as a top-level `display_name` field on the submit request.
         */
        data class DisplayName(
            val suggestedName: String?,
        ): Content

        data object End: Content
    }

    sealed interface NextPageRule {
        val nextPageId: String

        data class Thumbs(
            override val nextPageId: String,
            val isThumbsUp: Boolean
        ): NextPageRule

        data class Stars(
            override val nextPageId: String,
            val starRating: Int,
        ): NextPageRule

        data class Option(
            override val nextPageId: String,
            val optionId: String,
        ): NextPageRule

        data class None(
            override val nextPageId: String
        ): NextPageRule
    }
}
