package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState

/** Enforced client-side; matches the server's `suggested_display_name` cap. */
private const val DISPLAY_NAME_MAX_LENGTH = 40
private const val DISPLAY_NAME_PLACEHOLDER = "Your name"

@Composable
internal fun DisplayNameContentView(
    content: FormPageUiState.Content.DisplayName,
    onUpdateText: (String) -> Unit,
) {
    WandKitFilledTextField(
        value = content.name,
        onValueChange = { onUpdateText(it.take(DISPLAY_NAME_MAX_LENGTH)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        placeholder = DISPLAY_NAME_PLACEHOLDER,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        autoFocus = true,
        singleLine = true,
    )
}

@Preview
@Composable
private fun DisplayNameFormPagePreview() {
    FormPagePreview(
        FormPageUiState(
            id = "display-name",
            title = "What should we call you?",
            subtitle = "This name will show on your feedback posts.",
            imageUrl = null,
            content = FormPageUiState.Content.DisplayName(
                name = "Alex",
            ),
            buttons = listOf(
                FormPageButton(
                    label = "Continue",
                    type = FormPageButton.Type.PRIMARY,
                    action = FormPageButton.Action.CONTINUE,
                ),
                FormPageButton(
                    label = "Skip",
                    type = FormPageButton.Type.SECONDARY,
                    action = FormPageButton.Action.SKIP,
                )
            ),
            promoLabel = "Powered by WandKit",
        )
    )
}
