package com.flabbergast.wandkit.ui.compose.screenshotPrompt

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.flabbergast.wandkit.ui.compose.feedbackForm.content.WandKitOutlinedTextField
import com.flabbergast.wandkit.ui.compose.shared.WandKitButton
import com.flabbergast.wandkit.ui.compose.shared.WandKitButtonColors
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
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(component) {
                    detectTapGestures(onTap = {
                        component.onDismiss()
                    })
                }
        )

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding()
                .border(1.dp, WandKitColors.quaternaryLabel, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .widthIn(max = 560.dp),
            color = WandKitColors.systemBackground,
            contentColor = WandKitColors.label,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                image?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Your screenshot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, WandKitColors.quaternaryLabel, RoundedCornerShape(12.dp)),
                    )
                }

                when (phase) {
                    is ScreenshotPromptComponent.ViewState.Phase.Prompt -> ScreenshotPromptCardContent(
                        onDismiss = component::onDismiss,
                        onReport = component::onReport,
                    )

                    is ScreenshotPromptComponent.ViewState.Phase.Composing -> ScreenshotComposingContent(
                        phase = phase,
                        component = component,
                    )

                    is ScreenshotPromptComponent.ViewState.Phase.Sent -> ScreenshotSentContent()
                }
            }
        }
    }
}

@Composable
private fun ScreenshotPromptCardContent(
    onDismiss: () -> Unit,
    onReport: () -> Unit,
) {
    Text(
        text = "Report a problem?",
        style = WandKitTypography.titleMedium,
        textAlign = TextAlign.Center,
    )

    Text(
        text = "Attach this screenshot to a report so we can take a look.",
        style = WandKitTypography.bodyMedium,
        color = WandKitColors.secondaryLabel,
        textAlign = TextAlign.Center,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WandKitButton(
            text = "Not now",
            onClick = onDismiss,
            colors = WandKitButtonColors.Secondary,
            modifier = Modifier.weight(1f),
        )
        WandKitButton(
            text = "Report a problem",
            onClick = onReport,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScreenshotComposingContent(
    phase: ScreenshotPromptComponent.ViewState.Phase.Composing,
    component: ScreenshotPromptComponent,
) {
    Text(
        text = "Report a problem?",
        style = WandKitTypography.titleMedium,
        textAlign = TextAlign.Center,
    )

    WandKitOutlinedTextField(
        value = phase.text,
        onValueChange = component::onTextChanged,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6,
        placeholder = { Text("What went wrong?") },
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

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WandKitButton(
            text = "Not now",
            onClick = component::onDismiss,
            colors = WandKitButtonColors.Secondary,
            modifier = Modifier.weight(1f),
            enabled = !phase.isSending,
        )
        WandKitButton(
            text = if (phase.error != null) "Try again" else "Send",
            onClick = component::onSend,
            modifier = Modifier.weight(1f),
            enabled = phase.text.isNotBlank() && !phase.isSending,
            isLoading = phase.isSending,
        )
    }
}

@Composable
private fun ScreenshotSentContent() {
    Text(
        text = "Thanks — we got it",
        style = WandKitTypography.titleMedium,
        textAlign = TextAlign.Center,
    )

    Text(
        text = "The team will take a look.",
        style = WandKitTypography.bodyMedium,
        color = WandKitColors.secondaryLabel,
        textAlign = TextAlign.Center,
    )
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
