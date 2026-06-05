package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.formPage.FormPageComponent
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.EndContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.MultiChoiceContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.StarsContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.TextContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.ThumbsContentView

@Composable
internal fun FormPageView(
    component: FormPageComponent,
) {
    val state by component.viewState.subscribeAsState()

    val page = state.page ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick = component::dismissForm,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WandKitColors.onSurface,
                ),
            ) {
                Text("Close")
            }
        }

        page.imageUrl?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WandKitColors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Image",
                    style = WandKitTypography.bodyMedium,
                    color = WandKitColors.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = page.title,
                style = WandKitTypography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            page.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = WandKitTypography.bodyLarge,
                    color = WandKitColors.onSurfaceVariant,
                )
            }
        }

        FormPageContent(
            page = page,
            component = component,
        )

        Button(
            onClick = component::submitPage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = WandKitColors.primary,
                contentColor = WandKitColors.onPrimary,
            ),
        ) {
            Text(page.nextButtonLabel ?: defaultButtonLabel(page.content))
        }
    }
}

@Composable
private fun FormPageContent(
    page: FormPageUiState,
    component: FormPageComponent,
) {
    when (val content = page.content) {
        is FormPageUiState.Content.End -> EndContentView(page)
        is FormPageUiState.Content.MultiChoice -> MultiChoiceContentView(content, component::updateMultiChoice)
        is FormPageUiState.Content.Stars -> StarsContentView(content, component::updateStars)
        is FormPageUiState.Content.Text -> TextContentView(content, component::updateText)
        is FormPageUiState.Content.Thumbs -> ThumbsContentView(content, component::updateThumbs)
    }
}

private fun defaultButtonLabel(content: FormPageUiState.Content): String = when (content) {
    is FormPageUiState.Content.End -> "Done"
    else -> "Continue"
}
