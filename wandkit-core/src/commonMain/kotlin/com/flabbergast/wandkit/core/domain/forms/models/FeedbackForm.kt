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
