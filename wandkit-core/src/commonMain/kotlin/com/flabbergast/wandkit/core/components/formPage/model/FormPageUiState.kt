package com.flabbergast.wandkit.core.components.formPage.model

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId

public data class FormPageUiState(
    val id: FeedbackFormPageId,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val nextButtonLabel: String?,
    val content: Content,
) {
    public sealed interface Content {
        public data class Thumbs(
            val isThumbsUp: Boolean?,
        ): Content

        public data class Stars(
            val selectedStars: Int?,
            val totalStars: Int,
        ): Content

        public data class MultiChoice(
            val choices: List<Option>,
            val allowMultiple: Boolean,
        ): Content {
            public data class Option(val id: String, val label: String, val isSelected: Boolean)
        }

        public data class Text(
            val placeholder: String,
            val maxLength: Int,
            val text: String,
        ): Content

        public data object End: Content
    }
}
