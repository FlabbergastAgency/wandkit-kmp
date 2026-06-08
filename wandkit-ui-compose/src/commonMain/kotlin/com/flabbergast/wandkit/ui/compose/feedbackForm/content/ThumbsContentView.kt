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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.flabbergast.wandkit.ui.compose.Res
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.ic_thumbs_down_filled
import com.flabbergast.wandkit.ui.compose.ic_thumbs_up_filled
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ThumbsContentView(
    content: FormPageUiState.Content.Thumbs,
    onUpdateThumbs: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        ThumbsChoiceCard(
            resource = Res.drawable.ic_thumbs_up_filled,
            selected = content.isThumbsUp == true,
            onClick = { onUpdateThumbs(true) },
        )
        ThumbsChoiceCard(
            resource = Res.drawable.ic_thumbs_down_filled,
            selected = content.isThumbsUp == false,
            onClick = { onUpdateThumbs(false) },
        )
    }
}

@Composable
private fun ThumbsChoiceCard(
    resource: DrawableResource,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) WandKitColors.link else WandKitColors.secondarySystemFill
    val contentColor = if (selected) WandKitColors.label else WandKitColors.tintColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThumbsFormPagePreview() {
    FormPagePreview(
        FormPageUiState(
            id = "thumbs",
            title = "How was your experience?",
            subtitle = "A quick rating helps us understand overall sentiment.",
            imageUrl = null,
            content = FormPageUiState.Content.Thumbs(isThumbsUp = true),
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
