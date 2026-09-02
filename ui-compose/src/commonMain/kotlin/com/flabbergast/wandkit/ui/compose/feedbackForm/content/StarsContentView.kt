package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.Res
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.ic_star_filled
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun StarsContentView(
    content: FormPageUiState.Content.Stars,
    onUpdateStars: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 40.dp, bottom = 24.dp),
    ) {
        (1..content.totalStars).forEach { rating ->
            RatingStar(
                selected = content.selectedStars?.let { rating <= it } ?: false,
                onClick = { onUpdateStars(rating) },
            )
        }
    }
}

@Composable
private fun RatingStar(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tintColor = if (selected) WandKitColors.gold else WandKitColors.modalContentFill
    Icon(
        painter = painterResource(Res.drawable.ic_star_filled),
        contentDescription = null,
        tint = tintColor,
        modifier = Modifier
            .size(48.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = null
            )
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
            content = FormPageUiState.Content.Stars(
                selectedStars = 4,
                totalStars = 5,
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
