package com.flabbergast.wandkit.ui.compose.screenshotPrompt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.screenshotPrompt.ScreenshotPromptComponent
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitThemeDefaults
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.WandKitFilledTextField
import com.flabbergast.wandkit.ui.compose.shared.WandKitButton
import com.flabbergast.wandkit.ui.compose.shared.WandKitButtonColors
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalCard
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalCloseButton
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalScrim
import org.jetbrains.compose.resources.decodeToImageBitmap

/** iOS system red - there's no dedicated error token in [WandKitColors] yet. */
private val ScreenshotReportErrorColor = Color(0xFFFF3B30)

@Composable
internal fun ScreenshotPromptView(
    component: ScreenshotPromptComponent,
    contentAlignment: Alignment,
) {
    val state by component.viewState.subscribeAsState()
    val attachment = state.attachment ?: return
    val phase = state.phase

    val image = remember(attachment) {
        runCatching { attachment.data.decodeToImageBitmap() }.getOrNull()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = contentAlignment,
    ) {
        WandKitModalScrim(onDismiss = component::onDismiss)

        WandKitModalCard {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        WandKitModalCloseButton(
                            onClick = component::onDismiss,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        when (phase) {
                            is ScreenshotPromptComponent.ViewState.Phase.Prompt -> {
                                ScreenshotPromptCardContent()
                                image?.let {
                                    Image(
                                        bitmap = it,
                                        contentDescription = "Your screenshot",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .widthIn(max = 94.dp)
                                            .heightIn(max = 204.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                }
                            }

                            is ScreenshotPromptComponent.ViewState.Phase.Composing -> ScreenshotComposingContent(
                                phase = phase,
                                component = component,
                            )

                            is ScreenshotPromptComponent.ViewState.Phase.Sent -> ScreenshotSentContent()
                        }
                    }
                }

                when (phase) {
                    is ScreenshotPromptComponent.ViewState.Phase.Prompt -> {
                        ScreenshotActions(
                            primaryLabel = "Continue",
                            onPrimary = component::onReport,
                            secondaryLabel = "Not now",
                            onSecondary = component::onDismiss,
                        )
                    }

                    is ScreenshotPromptComponent.ViewState.Phase.Composing -> {
                        ScreenshotActions(
                            primaryLabel = if (phase.error != null) "Try again" else "Send",
                            onPrimary = component::onSend,
                            secondaryLabel = "Not now",
                            onSecondary = component::onDismiss,
                            primaryEnabled = phase.text.isNotBlank() && !phase.isSending,
                            secondaryEnabled = !phase.isSending,
                            isLoading = phase.isSending,
                        )
                    }

                    is ScreenshotPromptComponent.ViewState.Phase.Sent -> {
                        Text(
                            text = "Powered by WandKit",
                            style = WandKitTypography.modalPromoLabel,
                            textAlign = TextAlign.Center,
                            color = WandKitColors.label,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotPromptCardContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Report a problem?",
            style = WandKitTypography.modalTitle,
            textAlign = TextAlign.Center,
            color = WandKitColors.label,
        )

        Text(
            text = "Attach this screenshot to report so we can take a look.",
            style = WandKitTypography.modalSubtitle,
            color = WandKitColors.label,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScreenshotComposingContent(
    phase: ScreenshotPromptComponent.ViewState.Phase.Composing,
    component: ScreenshotPromptComponent,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Report a problem?",
            style = WandKitTypography.modalTitle,
            textAlign = TextAlign.Center,
            color = WandKitColors.label,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        WandKitFilledTextField(
            value = phase.text,
            onValueChange = component::onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = "What went wrong?",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            autoFocus = true,
        )

        phase.error?.let { error ->
            Text(
                text = error,
                style = WandKitTypography.bodySmall,
                color = ScreenshotReportErrorColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScreenshotSentContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Thanks — we got it",
            style = WandKitTypography.modalTitle,
            textAlign = TextAlign.Center,
            color = WandKitColors.label,
        )

        Text(
            text = "The team will take a look.",
            style = WandKitTypography.modalSubtitle,
            color = WandKitColors.label,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScreenshotActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WandKitButton(
                text = primaryLabel,
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
                enabled = primaryEnabled,
                isLoading = isLoading,
            )
            WandKitButton(
                text = secondaryLabel,
                onClick = onSecondary,
                colors = WandKitButtonColors.Secondary,
                modifier = Modifier.fillMaxWidth(),
                enabled = secondaryEnabled,
            )
        }

        Text(
            text = "Powered by WandKit",
            style = WandKitTypography.modalPromoLabel,
            textAlign = TextAlign.Center,
            color = WandKitColors.label,
        )
    }
}

@Preview
@Composable
private fun ScreenshotPromptViewPreviewLight() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        ScreenshotPromptView(PreviewScreenshotPromptComponent(), contentAlignment = Alignment.Center)
    }
}

@Preview
@Composable
private fun ScreenshotPromptViewPreviewDark() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.dark()) {
        ScreenshotPromptView(PreviewScreenshotPromptComponent(), contentAlignment = Alignment.Center)
    }
}

@Preview
@Composable
private fun ScreenshotPromptViewPreviewComposing() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        ScreenshotPromptView(
            PreviewScreenshotPromptComponent(
                phase = ScreenshotPromptComponent.ViewState.Phase.Composing(
                    text = "It crashes when I tap Save.",
                    isSending = false,
                    error = null,
                ),
            ),
            contentAlignment = Alignment.Center,
        )
    }
}

@Preview
@Composable
private fun ScreenshotPromptViewPreviewComposingError() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        ScreenshotPromptView(
            PreviewScreenshotPromptComponent(
                phase = ScreenshotPromptComponent.ViewState.Phase.Composing(
                    text = "It crashes when I tap Save.",
                    isSending = false,
                    error = "Couldn't send that. Please try again.",
                ),
            ),
            contentAlignment = Alignment.Center,
        )
    }
}

@Preview
@Composable
private fun ScreenshotPromptViewPreviewSent() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        ScreenshotPromptView(
            PreviewScreenshotPromptComponent(phase = ScreenshotPromptComponent.ViewState.Phase.Sent),
            contentAlignment = Alignment.Center,
        )
    }
}

private class PreviewScreenshotPromptComponent(
    phase: ScreenshotPromptComponent.ViewState.Phase = ScreenshotPromptComponent.ViewState.Phase.Prompt,
) : ScreenshotPromptComponent {
    // Preview data only: decoding is wrapped in runCatching, so a non-image
    // payload just skips the thumbnail instead of crashing.
    override val viewState: Value<ScreenshotPromptComponent.ViewState> =
        object : Value<ScreenshotPromptComponent.ViewState>() {
            private val state = ScreenshotPromptComponent.ViewState(
                attachment = WandKitComposerAttachment(
                    data = ByteArray(0),
                    contentType = "image/png",
                    fileName = "preview.png",
                ),
                phase = phase,
            )

            override val value: ScreenshotPromptComponent.ViewState get() = state

            override fun subscribe(observer: (ScreenshotPromptComponent.ViewState) -> Unit): Cancellation {
                observer(state)
                return Cancellation {}
            }
        }

    override fun onReport() = Unit

    override fun onTextChanged(text: String) = Unit

    override fun onSend() = Unit

    override fun onDismiss() = Unit
}
