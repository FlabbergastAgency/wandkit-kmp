package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography

@Composable
internal fun TextContentView(
    content: FormPageUiState.Content.Text,
    onUpdateText: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WandKitOutlinedTextField(
            value = content.text,
            onValueChange = { onUpdateText(it.take(content.maxLength)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            placeholder = { Text(content.placeholder) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Text(
            text = "${content.text.length}/${content.maxLength}",
            modifier = Modifier.fillMaxWidth(),
            style = WandKitTypography.bodySmall,
            color = WandKitColors.secondaryLabel,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun WandKitOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        minLines = minLines,
        maxLines = maxLines,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WandKitColors.tintColor,
            unfocusedBorderColor = WandKitColors.separator,
            focusedTextColor = WandKitColors.label,
            unfocusedTextColor = WandKitColors.label,
            cursorColor = WandKitColors.tintColor,
            focusedPlaceholderColor = WandKitColors.placeholderText,
            unfocusedPlaceholderColor = WandKitColors.placeholderText,
        ),
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
