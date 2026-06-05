package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography

@Composable
internal fun ThumbsContentView(
    content: FormPageUiState.Content.Thumbs,
    onUpdateThumbs: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThumbsChoiceCard(
            label = "Thumbs up",
            selected = content.isThumbsUp == true,
            modifier = Modifier.weight(1f),
            onClick = { onUpdateThumbs(true) },
        )
        ThumbsChoiceCard(
            label = "Thumbs down",
            selected = content.isThumbsUp == false,
            modifier = Modifier.weight(1f),
            onClick = { onUpdateThumbs(false) },
        )
    }
}

@Composable
private fun ThumbsChoiceCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) WandKitColors.secondaryContainer else WandKitColors.surfaceVariant
    val contentColor = if (selected) WandKitColors.onSecondaryContainer else WandKitColors.onSurfaceVariant

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
private fun ThumbsFormPagePreview() {
    FormPagePreview(
        FormPageUiState(
            id = "thumbs",
            title = "How was your experience?",
            subtitle = "A quick rating helps us understand overall sentiment.",
            imageUrl = null,
            nextButtonLabel = "Continue",
            content = FormPageUiState.Content.Thumbs(isThumbsUp = true),
        )
    )
}
