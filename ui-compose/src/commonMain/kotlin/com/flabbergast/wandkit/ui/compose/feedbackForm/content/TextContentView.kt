package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalContentFillShape

@Composable
internal fun TextContentView(
    content: FormPageUiState.Content.Text,
    onUpdateText: (String) -> Unit,
) {
    WandKitFilledTextField(
        value = content.text,
        onValueChange = { onUpdateText(it.take(content.maxLength)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp)
            .heightIn(min = 240.dp),
        placeholder = content.placeholder,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        autoFocus = true,
        singleLine = false,
    )
}

/**
 * @param autoFocus Focus the field (and so raise the keyboard) as soon as it
 * enters composition. Use on pages where typing is the only thing to do, so
 * the user doesn't have to tap the box first.
 */
@Composable
internal fun WandKitFilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    autoFocus: Boolean = false,
    singleLine: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    val textStyle = WandKitTypography.modalFieldLabel.copy(
        color = WandKitColors.label,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(WandKitModalContentFillShape)
            .background(WandKitColors.modalContentFill, WandKitModalContentFillShape)
            .padding(16.dp)
            .focusRequester(focusRequester),
        textStyle = textStyle,
        cursorBrush = SolidColor(WandKitColors.label),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = WandKitColors.placeholderText),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Preview
@Composable
private fun TextFormPagePreview() {
    FormPagePreview(
        FormPageUiState(
            id = "text",
            title = "Tell us more",
            subtitle = "Anything we should improve or keep doing?",
            imageUrl = null,
            content = FormPageUiState.Content.Text(
                placeholder = "Share your thoughts...",
                maxLength = 140,
                text = "The onboarding was smooth and the UI felt polished.",
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
