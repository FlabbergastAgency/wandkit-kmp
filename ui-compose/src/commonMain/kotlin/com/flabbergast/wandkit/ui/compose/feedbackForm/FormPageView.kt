package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.formPage.FormPageComponent
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.DisplayNameContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.FormPagePreview
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.MultiChoiceContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.StarsContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.TextContentView
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.ThumbsContentView
import com.flabbergast.wandkit.ui.compose.shared.WandKitButton
import com.flabbergast.wandkit.ui.compose.shared.WandKitButtonColors
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalCloseButton

@Composable
internal fun FormPageView(
    component: FormPageComponent,
) {
    val state by component.viewState.subscribeAsState()

    val page = state.page ?: return
    val focusManager = LocalFocusManager.current
    val clearFocusOnBackgroundTapModifier = if (
        page.content is FormPageUiState.Content.Text || page.content is FormPageUiState.Content.DisplayName
    ) {
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
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessVeryLow
                )
            )
            .then(clearFocusOnBackgroundTapModifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                WandKitModalCloseButton(
                    onClick = component::dismissForm,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = page.title,
                    style = WandKitTypography.modalTitle,
                    color = WandKitColors.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                page.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = WandKitTypography.modalSubtitle,
                        color = WandKitColors.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            FormPageContent(
                page = page,
                component = component,
            )

            if (page.buttons.isNotEmpty() || page.promoLabel != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (page.buttons.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            page.buttons.forEach { button ->
                                val usePrimary = page.buttons.size == 1 ||
                                    button.type == FormPageButton.Type.PRIMARY
                                WandKitButton(
                                    text = button.label,
                                    onClick = { component.buttonAction(button.action) },
                                    colors = if (usePrimary) {
                                        WandKitButtonColors.Primary
                                    } else {
                                        WandKitButtonColors.Secondary
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    page.promoLabel?.let { promoLabel ->
                        Text(
                            text = promoLabel,
                            style = WandKitTypography.modalPromoLabel,
                            textAlign = TextAlign.Center,
                            color = WandKitColors.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        )
                    }
                }
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
        is FormPageUiState.Content.DisplayName -> DisplayNameContentView(content, component::updateText)
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
