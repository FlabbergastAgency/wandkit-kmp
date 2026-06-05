package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors

@Composable
internal fun StarsContentView(
    content: FormPageUiState.Content.Stars,
    onUpdateStars: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        (1..content.totalStars).forEach { rating ->
            WandKitFilterChip(
                selected = content.selectedStars == rating,
                onClick = { onUpdateStars(rating) },
                label = { Text(rating.toString()) },
            )
        }
    }
}

@Composable
private fun WandKitFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = WandKitColors.surfaceVariant,
            labelColor = WandKitColors.onSurfaceVariant,
            selectedContainerColor = WandKitColors.secondaryContainer,
            selectedLabelColor = WandKitColors.onSecondaryContainer,
        ),
    )
}

@Preview
@Composable
private fun StarsFormPagePreview() {
    FormPagePreview(
        FormPageUiState(
            id = "stars",
            title = "How would you rate the app?",
            subtitle = "Pick a rating from one to five stars.",
            imageUrl = null,
            nextButtonLabel = "Continue",
            content = FormPageUiState.Content.Stars(
                selectedStars = 4,
                totalStars = 5,
            ),
        )
    )
}
