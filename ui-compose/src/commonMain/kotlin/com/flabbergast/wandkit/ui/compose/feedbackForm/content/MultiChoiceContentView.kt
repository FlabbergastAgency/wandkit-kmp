package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.Res
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.ic_check
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalContentFillShape
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MultiChoiceContentView(
    content: FormPageUiState.Content.MultiChoice,
    onUpdateMultiChoice: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content.choices.forEach { choice ->
            ChoiceCard(
                label = choice.label,
                selected = choice.isSelected,
                showCheckbox = content.allowMultiple,
                onClick = { onUpdateMultiChoice(choice.id) },
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    label: String,
    selected: Boolean,
    showCheckbox: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(WandKitModalContentFillShape)
            .background(WandKitColors.modalContentFill, WandKitModalContentFillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheckbox) {
            ChoiceCheckbox(selected = selected)
        }
        Text(
            text = label,
            style = WandKitTypography.modalFieldLabel,
            color = WandKitColors.label,
        )
    }
}

@Composable
private fun ChoiceCheckbox(selected: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(21.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(WandKitColors.modalCheckboxFill, shape)
                } else {
                    Modifier.border(1.5.dp, WandKitColors.modalCheckboxBorder, shape)
                }
            ),
    ) {
        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = null,
                tint = WandKitColors.modalPrimaryButtonContent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MultiChoiceFormPagePreview() {
    FormPagePreview(
        FormPageUiState(
            id = "choices",
            title = "What stood out the most?",
            subtitle = "Select one or more things you liked.",
            imageUrl = null,
            content = FormPageUiState.Content.MultiChoice(
                choices = listOf(
                    FormPageUiState.Content.MultiChoice.Option("speed", "Fast setup", true),
                    FormPageUiState.Content.MultiChoice.Option("design", "Clean design", false),
                    FormPageUiState.Content.MultiChoice.Option("support", "Helpful support", true),
                ),
                allowMultiple = true,
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
