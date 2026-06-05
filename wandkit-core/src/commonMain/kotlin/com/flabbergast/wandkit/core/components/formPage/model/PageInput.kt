package com.flabbergast.wandkit.core.components.formPage.model

internal data class PageInput(
    val text: String? = null,
    val stars: Int? = null,
    val optionIds: List<String>? = null,
    val isThumbsUp: Boolean? = null,
)