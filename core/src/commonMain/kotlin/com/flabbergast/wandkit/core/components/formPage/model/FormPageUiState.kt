package com.flabbergast.wandkit.core.components.formPage.model

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId

public data class FormPageUiState(
    val id: FeedbackFormPageId,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val content: Content,
    val buttons: List<FormPageButton>,
    val promoLabel: String?,
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

public data class FormPageButton(
    val label: String,
    val type: Type,
    val action: Action,
) {
    public enum class Type {
        PRIMARY, SECONDARY
    }

    public enum class Action {
        CONTINUE, SKIP
    }
}
