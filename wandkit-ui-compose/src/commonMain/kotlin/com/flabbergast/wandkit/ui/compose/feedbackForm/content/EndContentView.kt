package com.flabbergast.wandkit.ui.compose.feedbackForm.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography

@Composable
internal fun EndContentView(
    page: FormPageUiState,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WandKitColors.surfaceVariant,
        contentColor = WandKitColors.onSurfaceVariant,
    ) {
        Text(
            text = page.subtitle ?: "Thanks for sharing your feedback.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            style = WandKitTypography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun EndFormPagePreview() {
    FormPagePreview(
        FormPageUiState(
            id = "end",
            title = "Thanks for the feedback",
            subtitle = "We appreciate you taking the time to share it with us.",
            imageUrl = null,
            nextButtonLabel = "Done",
            content = FormPageUiState.Content.End,
        )
    )
}
