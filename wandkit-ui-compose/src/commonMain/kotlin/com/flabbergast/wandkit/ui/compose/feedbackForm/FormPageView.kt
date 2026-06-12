package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.formPage.FormPageComponent
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.Res
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.FormPagePreview
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.MultiChoiceContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.StarsContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.TextContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.ThumbsContentView
import com.flabbergast.wandkit.ui.compose.ic_close
import com.flabbergast.wandkit.ui.compose.shared.WandKitButton
import com.flabbergast.wandkit.ui.compose.shared.WandKitButtonColors
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun FormPageView(
    component: FormPageComponent,
) {
    val state by component.viewState.subscribeAsState()

    val page = state.page ?: return
    val focusManager = LocalFocusManager.current
    val clearFocusOnBackgroundTapModifier = if (page.content is FormPageUiState.Content.Text) {
        Modifier.pointerInput(page.id) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WandKitColors.secondarySystemBackground)
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessVeryLow
                )
            )
            .then(clearFocusOnBackgroundTapModifier),
    ) {
        IconButton(
            onClick = component::dismissForm,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = WandKitColors.label,
            ),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = page.title,
                style = WandKitTypography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            page.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = WandKitTypography.bodyLarge,
                    color = WandKitColors.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            FormPageContent(
                page = page,
                component = component,
            )

            if (page.buttons.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                page.buttons.forEach { button ->
                    WandKitButton(
                        text = button.label,
                        onClick = { component.buttonAction(button.action) },
                        colors = when (button.type) {
                            FormPageButton.Type.PRIMARY -> WandKitButtonColors.Primary
                            FormPageButton.Type.SECONDARY -> WandKitButtonColors.Secondary
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            page.promoLabel?.let { promoLabel ->
                Text(
                    text = promoLabel,
                    style = WandKitTypography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = WandKitColors.tertiaryLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun FormPageContent(
    page: FormPageUiState,
    component: FormPageComponent,
) {
    when (val content = page.content) {
        is FormPageUiState.Content.End -> Unit
        is FormPageUiState.Content.MultiChoice -> MultiChoiceContentView(
            content,
            component::updateMultiChoice
        )

        is FormPageUiState.Content.Stars -> StarsContentView(content, component::updateStars)
        is FormPageUiState.Content.Text -> TextContentView(content, component::updateText)
        is FormPageUiState.Content.Thumbs -> ThumbsContentView(content, component::updateThumbs)
    }
}

@Preview
@Composable
private fun FormPageViewPreview() {
    WandKitThemeProvider {
        FormPagePreview(
            page = FormPageUiState(
                id = "123",
                title = "Welcome to the form page",
                subtitle = "This is the subtitle",
                imageUrl = null,
                content = FormPageUiState.Content.Stars(4, 5),
                buttons = listOf(
                    FormPageButton(
                        label = "Continue",
                        type = FormPageButton.Type.PRIMARY,
                        action = FormPageButton.Action.CONTINUE,
                    )
                ),
                promoLabel = "Powered by WandKit",
            )
        )
    }
}
