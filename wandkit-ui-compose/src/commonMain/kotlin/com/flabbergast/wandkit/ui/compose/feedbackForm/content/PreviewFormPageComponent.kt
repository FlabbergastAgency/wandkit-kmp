package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.formPage.FormPageComponent
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitThemeDefaults
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.feedbackForm.FormPageView

@Composable
internal fun FormPagePreview(
    page: FormPageUiState,
) {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        FormPageView(PreviewFormPageComponent(page))
    }
}

private class PreviewFormPageComponent(
    page: FormPageUiState,
) : FormPageComponent {
    override val viewState: Value<FormPageComponent.ViewState> = object : Value<FormPageComponent.ViewState>() {
        private val state = FormPageComponent.ViewState(page)

        override val value: FormPageComponent.ViewState
            get() = state

        override fun subscribe(observer: (FormPageComponent.ViewState) -> Unit): Cancellation {
            observer(state)
            return Cancellation {}
        }
    }

    override fun dismissForm() = Unit

    override fun submitPage() = Unit

    override fun updateThumbs(isThumbsUp: Boolean) = Unit

    override fun updateStars(stars: Int) = Unit

    override fun updateText(text: String) = Unit

    override fun updateMultiChoice(optionId: String) = Unit
}
