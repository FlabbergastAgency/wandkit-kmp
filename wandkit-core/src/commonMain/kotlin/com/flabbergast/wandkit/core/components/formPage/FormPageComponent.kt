package com.flabbergast.wandkit.core.components.formPage

import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState

public interface FormPageComponent {
    public val viewState: Value<ViewState>

    public fun dismissForm()
    public fun submitPage()

    public fun updateThumbs(isThumbsUp: Boolean)
    public fun updateStars(stars: Int)
    public fun updateText(text: String)
    public fun updateMultiChoice(optionId: String)

    public data class ViewState(
        val page: FormPageUiState?
    )
}