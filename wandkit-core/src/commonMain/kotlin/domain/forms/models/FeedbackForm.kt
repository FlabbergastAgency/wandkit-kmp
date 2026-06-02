package com.flabbergast.wandkit.core.domain.forms.models

internal data class FeedbackForm(
    val formId: String,
    val impressionId: String,
    val entryPage: FeedbackFormPage,
    val pages: Map<FeedbackFormPageId, FeedbackFormPage>,
    val description: String?,
)

internal typealias FeedbackFormPageId = String

internal sealed interface FeedbackFormPage {
    val id: FeedbackFormPageId
    val title: String
    val subtitle: String?
    val imageUrl: String?
    val nextButtonLabel: String?
    val isRequired: Boolean
    val next: List<NextPageRule>

    data class Thumbs(
        override val id: String,
        override val title: String,
        override val subtitle: String?,
        override val imageUrl: String?,
        override val nextButtonLabel: String?,
        override val isRequired: Boolean,
        override val next: List<NextPageRule>,
    ): FeedbackFormPage

    data class Stars(
        override val id: String,
        override val title: String,
        override val subtitle: String?,
        override val imageUrl: String?,
        override val nextButtonLabel: String?,
        override val isRequired: Boolean,
        override val next: List<NextPageRule>,
        val starCount: Int,
    ): FeedbackFormPage

    data class MultiChoice(
        override val id: String,
        override val title: String,
        override val subtitle: String?,
        override val imageUrl: String?,
        override val nextButtonLabel: String?,
        override val isRequired: Boolean,
        override val next: List<NextPageRule>,
        val options: List<Option>,
        val allowMultiple: Boolean,
    ): FeedbackFormPage {
        data class Option(
            val id: String,
            val label: String,
        )
    }

    data class Text(
        override val id: String,
        override val title: String,
        override val subtitle: String?,
        override val imageUrl: String?,
        override val nextButtonLabel: String?,
        override val isRequired: Boolean,
        override val next: List<NextPageRule>,
        val placeholder: String,
        val maxLength: Int,
    ): FeedbackFormPage

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
