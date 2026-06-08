package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography

@Composable
internal fun MultiChoiceContentView(
    content: FormPageUiState.Content.MultiChoice,
    onUpdateMultiChoice: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content.choices.forEach { choice ->
            ChoiceRow(
                label = choice.label,
                selected = choice.isSelected,
                onClick = { onUpdateMultiChoice(choice.id) },
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ChoiceCard(
        label = label,
        selected = selected,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    )
}

@Composable
private fun ChoiceCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) WandKitColors.secondarySystemFill else WandKitColors.secondarySystemBackground
    val contentColor = if (selected) WandKitColors.label else WandKitColors.secondaryLabel

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (selected) contentColor else contentColor.copy(alpha = 0.25f)),
            )
            Text(
                text = label,
                style = WandKitTypography.titleMedium,
                color = contentColor,
            )
        }
    }
}

@Preview
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
