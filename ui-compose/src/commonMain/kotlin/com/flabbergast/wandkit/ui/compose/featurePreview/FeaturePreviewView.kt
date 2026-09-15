package com.flabbergast.wandkit.ui.compose.featurePreview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.featurepreview.FeaturePreviewComponent
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitThemeDefaults
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.WandKitTypography
import com.flabbergast.wandkit.ui.compose.shared.WandKitButton
import com.flabbergast.wandkit.ui.compose.shared.WandKitButtonColors

/** iOS system red - there's no dedicated error token in [WandKitColors] yet. */
private val FeaturePreviewErrorColor = Color(0xFFFF3B30)
private val BackdropScrimColor = Color.Black.copy(alpha = 0.35f)

@Composable
internal fun FeaturePreviewView(
    component: FeaturePreviewComponent,
) {
    val state by component.viewState.subscribeAsState()
    val content = state.content ?: return
    val uriHandler = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimColor)
            .pointerInput(component) {
                detectTapGestures(onTap = { component.onDismiss() })
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = WandKitColors.systemBackground.copy(alpha = 0.92f),
            contentColor = WandKitColors.label,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FeaturePreviewIconBadge(isAvailable = content is FeaturePreviewComponent.ViewState.Content.Available)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = content.title,
                    style = WandKitTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = content.message,
                    style = WandKitTypography.bodyLarge,
                    color = WandKitColors.secondaryLabel,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (content) {
                    is FeaturePreviewComponent.ViewState.Content.ComingSoon -> ComingSoonButtons(
                        content = content,
                        onPrimary = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            component.onPrimary()
                        },
                        onSecondary = component::onSecondary,
                    )

                    is FeaturePreviewComponent.ViewState.Content.Available -> AvailableButtons(
                        content = content,
                        onPrimary = {
                            content.storeUrl?.let(uriHandler::openUri)
                            component.onPrimary()
                        },
                        onSecondary = component::onSecondary,
                    )

                    is FeaturePreviewComponent.ViewState.Content.Generic -> GenericButtons(
                        content = content,
                        onPrimary = component::onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoonButtons(
    content: FeaturePreviewComponent.ViewState.Content.ComingSoon,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    WandKitButton(
        text = content.primaryLabel,
        onClick = onPrimary,
        colors = WandKitButtonColors.Primary,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = content.isPrimaryEnabled,
        isLoading = content.isVoting,
    )

    Spacer(modifier = Modifier.height(12.dp))

    WandKitButton(
        text = content.secondaryLabel,
        onClick = onSecondary,
        colors = WandKitButtonColors.Secondary,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    )

    content.error?.let { error ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = WandKitTypography.bodySmall,
            color = FeaturePreviewErrorColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AvailableButtons(
    content: FeaturePreviewComponent.ViewState.Content.Available,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    if (content.storeUrl != null) {
        WandKitButton(
            text = content.primaryLabel,
            onClick = onPrimary,
            colors = WandKitButtonColors.Primary,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    WandKitButton(
        text = content.secondaryLabel,
        onClick = onSecondary,
        colors = WandKitButtonColors.Secondary,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    )
}

@Composable
private fun GenericButtons(
    content: FeaturePreviewComponent.ViewState.Content.Generic,
    onPrimary: () -> Unit,
) {
    WandKitButton(
        text = content.primaryLabel,
        onClick = onPrimary,
        colors = WandKitButtonColors.Primary,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    )
}

@Composable
private fun FeaturePreviewIconBadge(isAvailable: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val tint = WandKitColors.link
        Canvas(modifier = Modifier.size(24.dp)) {
            if (isAvailable) {
                val strokeWidth = size.minDimension * 0.14f
                val path = Path().apply {
                    moveTo(size.width * 0.20f, size.height * 0.55f)
                    lineTo(size.width * 0.42f, size.height * 0.76f)
                    lineTo(size.width * 0.82f, size.height * 0.28f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            } else {
                val dotRadius = size.minDimension * 0.075f
                drawCircle(color = tint, radius = dotRadius, center = Offset(size.width / 2f, size.height * 0.22f))
                drawLine(
                    color = tint,
                    start = Offset(size.width / 2f, size.height * 0.42f),
                    end = Offset(size.width / 2f, size.height * 0.82f),
                    strokeWidth = dotRadius * 1.7f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Preview
@Composable
private fun FeaturePreviewViewPreviewComingSoonLight() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        FeaturePreviewView(PreviewFeaturePreviewComponent(comingSoon()))
    }
}

@Preview
@Composable
private fun FeaturePreviewViewPreviewComingSoonDark() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.dark()) {
        FeaturePreviewView(PreviewFeaturePreviewComponent(comingSoon()))
    }
}

@Preview
@Composable
private fun FeaturePreviewViewPreviewComingSoonError() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        FeaturePreviewView(
            PreviewFeaturePreviewComponent(comingSoon(error = "Couldn't save, please try again")),
        )
    }
}

@Preview
@Composable
private fun FeaturePreviewViewPreviewAvailable() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        FeaturePreviewView(
            PreviewFeaturePreviewComponent(
                FeaturePreviewComponent.ViewState.Content.Available(
                    title = "It's here",
                    message = "Update the app to use this feature.",
                    secondaryLabel = "Continue with EASA",
                    primaryLabel = "Update app",
                    storeUrl = "https://play.google.com/store/apps/details?id=com.example",
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun FeaturePreviewViewPreviewGeneric() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.light()) {
        FeaturePreviewView(
            PreviewFeaturePreviewComponent(
                FeaturePreviewComponent.ViewState.Content.Generic(
                    title = "This feature is coming soon",
                    message = "We're still working on it. Check back in a future update.",
                    primaryLabel = "Close",
                ),
            ),
        )
    }
}

private fun comingSoon(
    primaryLabel: String = "Let me know when FAA is ready",
    isPrimaryEnabled: Boolean = true,
    isVoting: Boolean = false,
    error: String? = null,
) = FeaturePreviewComponent.ViewState.Content.ComingSoon(
    title = "FAA support is almost here",
    message = "FAA support is coming in about 2 weeks. Start with EASA now, and switch to FAA later without losing your flight data.",
    secondaryLabel = "Continue with EASA",
    primaryLabel = primaryLabel,
    isPrimaryEnabled = isPrimaryEnabled,
    isVoting = isVoting,
    error = error,
)

private class PreviewFeaturePreviewComponent(
    content: FeaturePreviewComponent.ViewState.Content,
) : FeaturePreviewComponent {
    override val viewState: Value<FeaturePreviewComponent.ViewState> =
        object : Value<FeaturePreviewComponent.ViewState>() {
            private val state = FeaturePreviewComponent.ViewState(content = content)

            override val value: FeaturePreviewComponent.ViewState get() = state

            override fun subscribe(observer: (FeaturePreviewComponent.ViewState) -> Unit): Cancellation {
                observer(state)
                return Cancellation {}
            }
        }

    override fun onPrimary() = Unit

    override fun onSecondary() = Unit

    override fun onDismiss() = Unit
}
