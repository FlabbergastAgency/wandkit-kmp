package com.flabbergast.wandkit.ui.compose.accessGate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateViewState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitThemeDefaults
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.replay.wandKitReplayMasked

/** Test tags, exposed as resource ids by the gate Activity so UI Automator can find them. */
internal object AccessGateTestTags {
    const val CODE_FIELD = "wandkit.accessGate.codeField"
    const val SUBMIT_BUTTON = "wandkit.accessGate.submitButton"
    const val ERROR = "wandkit.accessGate.error"
    const val RETRY_BUTTON = "wandkit.accessGate.retryButton"
    const val HELP_BUTTON = "wandkit.accessGate.helpButton"
    const val PROGRESS = "wandkit.accessGate.progress"
}

/**
 * The screen's own labels; title, message and errors come with the view
 * state. The offline screen has fixed copy - the server title is about
 * the invite, not the connection.
 */
private object AccessGateLabels {
    const val PLACEHOLDER = "Invite code"
    const val BUTTON = "Continue"
    const val HELP = "Need a code?"
    const val OFFLINE_TITLE = "You're offline"
    const val OFFLINE_MESSAGE = "Connect to the internet and try again."
    const val RETRY = "Try again"
}

private val ContentMaxWidth = 420.dp
private val HorizontalPadding = 24.dp
private val ControlHeight = 56.dp
private val FieldShape = RoundedCornerShape(24.dp)

/** Below this the pinned actions would squeeze the top group out of sight. */
private val CompactHeight = 360.dp
private const val FADE_IN_MILLIS = 220
private const val FADE_OUT_MILLIS = 150

/** Everything the screen paints, derived once from the theme and the accent. */
@Immutable
private data class AccessGatePalette(
    val accent: Color,
    val onAccent: Color,
    /** The accent where it is text or a glyph on the background - see [AccessGateColors.legibleAccent]. */
    val accentForeground: Color,
    val background: Color,
    val iconBubble: Color,
    val field: Color,
    val label: Color,
    val secondaryLabel: Color,
    val placeholder: Color,
    val error: Color,
)

@Composable
private fun accessGatePalette(accent: Color): AccessGatePalette {
    val colors = WandKitColors
    val background = colors.systemBackground
    val isDark = AccessGateColors.relativeLuminance(background) < 0.5f
    return remember(accent, colors) {
        AccessGatePalette(
            accent = accent,
            onAccent = AccessGateColors.onAccent(accent),
            accentForeground = AccessGateColors.legibleAccent(accent, background, fallback = colors.label),
            background = background,
            iconBubble = accent.copy(alpha = 0.14f).compositeOver(background),
            field = colors.secondarySystemBackground,
            label = colors.label,
            secondaryLabel = colors.secondaryLabel,
            placeholder = colors.placeholderText,
            // iOS system red, light/dark - there's no error token in the colour scheme yet.
            error = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30),
        )
    }
}

/**
 * The invite gate screen: a spinner while checking, the code form, or the
 * offline state with "Try again". Full screen and opaque on purpose - it hides
 * the host app entirely - with a soft glow of [accent] at the top.
 *
 * Stateless: [code] is hoisted so it survives the offline detour and
 * configuration changes in the caller's saved state.
 *
 * @param accent The host's brand accent; the SDK theme's tint when it has none.
 */
@OptIn(InternalWandKitApi::class)
@Composable
internal fun AccessGateView(
    state: WandKitAccessGateViewState,
    code: String,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onOpenHelp: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = WandKitColors.tintColor,
) {
    val palette = accessGatePalette(accent)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .accessGateGlow(palette.accent),
    ) {
        // Keyed on the phase only: title, errors and the submitting flag
        // update in place without a cross-fade.
        AnimatedContent(
            targetState = state.phase,
            transitionSpec = {
                fadeIn(tween(FADE_IN_MILLIS)) togetherWith fadeOut(tween(FADE_OUT_MILLIS))
            },
            modifier = Modifier.fillMaxSize(),
            label = "accessGatePhase",
        ) { phase ->
            when (phase) {
                WandKitAccessGateViewState.Phase.Checking -> CheckingContent(palette)
                WandKitAccessGateViewState.Phase.Offline -> OfflineContent(palette, onRetry)
                WandKitAccessGateViewState.Phase.CodeEntry -> CodeEntryContent(
                    state = state,
                    code = code,
                    palette = palette,
                    onCodeChange = onCodeChange,
                    onSubmit = onSubmit,
                    onOpenHelp = onOpenHelp,
                )
                // The Activity finishes on this; the backdrop alone meanwhile.
                WandKitAccessGateViewState.Phase.Hidden -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * The accent glow: an ellipse 140% of the width and 560 dp tall, centred
 * 20 dp below the top edge (so 260 dp of it lies above the screen), fading
 * from 34% of the accent to nothing. Stops at the accent's own hue with zero
 * alpha rather than [Color.Transparent], which would fade through grey.
 */
private fun Modifier.accessGateGlow(accent: Color): Modifier = drawBehind {
    val radiusY = 280.dp.toPx()
    val radiusX = size.width * 0.7f
    val center = Offset(size.width / 2f, 20.dp.toPx())
    val brush = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0f)),
        center = center,
        radius = radiusY,
    )
    scale(scaleX = radiusX / radiusY, scaleY = 1f, pivot = center) {
        drawCircle(brush = brush, radius = radiusY, center = center)
    }
}

/**
 * Two regions: [top] centred in the space above [actions], scrolling when it
 * does not fit, and [actions] pinned to the bottom - 12 dp above the keyboard
 * when it is up, 8 dp above the navigation bar when it is down.
 *
 * When even that leaves too little room for the top group (a phone in
 * landscape with the keyboard up), the actions join the scrolling content
 * right under it instead: the focused field stays in view, the button is a
 * short scroll away, and the keyboard's Done key submits.
 * The top group stays at the same place in the composition either way, so
 * the code field keeps its focus - and the keyboard - across the switch.
 */
@Composable
private fun GateScaffold(
    top: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomPadding = with(density) {
        if (imeBottom > navigationBottom) imeBottom.toDp() + 12.dp else navigationBottom.toDp() + 8.dp
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(bottom = bottomPadding),
    ) {
        val isCompact = maxHeight < CompactHeight
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .heightIn(min = maxHeight)
                        .padding(horizontal = HorizontalPadding, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = ContentMaxWidth)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        top()
                        if (isCompact) {
                            Spacer(Modifier.height(24.dp))
                            actions()
                        }
                    }
                }
            }
            if (!isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HorizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = ContentMaxWidth)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = actions,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckingContent(palette: AccessGatePalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(40.dp)
                .testTag(AccessGateTestTags.PROGRESS),
            color = palette.accentForeground,
            strokeWidth = 3.5.dp,
        )
    }
}

@Composable
private fun OfflineContent(
    palette: AccessGatePalette,
    onRetry: () -> Unit,
) {
    GateScaffold(
        top = {
            GateHeader(
                icon = AccessGateIcons.WifiOff,
                title = AccessGateLabels.OFFLINE_TITLE,
                message = AccessGateLabels.OFFLINE_MESSAGE,
                palette = palette,
            )
        },
        actions = {
            GatePrimaryButton(
                text = AccessGateLabels.RETRY,
                onClick = onRetry,
                palette = palette,
                modifier = Modifier.testTag(AccessGateTestTags.RETRY_BUTTON),
            )
        },
    )
}

@OptIn(InternalWandKitApi::class)
@Composable
private fun CodeEntryContent(
    state: WandKitAccessGateViewState,
    code: String,
    palette: AccessGatePalette,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenHelp: (url: String) -> Unit,
) {
    val canSubmit = code.isNotBlank() && !state.isSubmitting

    GateScaffold(
        top = {
            GateHeader(
                icon = AccessGateIcons.Lock,
                title = state.title,
                message = state.message,
                palette = palette,
            )
            AccessGateCodeField(
                value = code,
                onValueChange = onCodeChange,
                isError = state.errorMessage != null,
                palette = palette,
                onDone = { if (canSubmit) onSubmit() },
                modifier = Modifier.padding(top = 24.dp),
            )
            InlineError(message = state.errorMessage, palette = palette)
        },
        actions = {
            GatePrimaryButton(
                text = AccessGateLabels.BUTTON,
                onClick = onSubmit,
                palette = palette,
                enabled = code.isNotBlank(),
                isLoading = state.isSubmitting,
                modifier = Modifier.testTag(AccessGateTestTags.SUBMIT_BUTTON),
            )
            state.helpUrl?.let { url ->
                TextButton(
                    onClick = { onOpenHelp(url) },
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.accentForeground),
                    modifier = Modifier
                        // 48 dp touch target; the label sits ~16 dp under the button.
                        .padding(top = 2.dp)
                        .heightIn(min = 48.dp)
                        .testTag(AccessGateTestTags.HELP_BUTTON),
                ) {
                    Text(
                        text = AccessGateLabels.HELP,
                        style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                        color = palette.accentForeground,
                    )
                }
            }
        },
    )
}

@Composable
private fun GateHeader(
    icon: ImageVector,
    title: String,
    message: String,
    palette: AccessGatePalette,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(palette.iconBubble),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative: the title says it all.
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.accentForeground,
            modifier = Modifier.size(32.dp),
        )
    }
    Text(
        text = title,
        modifier = Modifier
            .padding(top = 20.dp)
            .semantics { heading() },
        style = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        color = palette.label,
        textAlign = TextAlign.Center,
    )
    Text(
        text = message,
        modifier = Modifier.padding(top = 12.dp),
        style = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
        color = palette.secondaryLabel,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AccessGateCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    palette: AccessGatePalette,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> palette.error
            isFocused -> palette.accent.copy(alpha = 0.7f)
            else -> palette.accent.copy(alpha = 0f)
        },
        label = "accessGateFieldBorder",
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ControlHeight)
            .clip(FieldShape)
            .background(palette.field, FieldShape)
            .border(1.5.dp, borderColor, FieldShape)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .testTag(AccessGateTestTags.CODE_FIELD)
            // The code is a credential of sorts: keep it out of session replay frames.
            .wandKitReplayMasked(),
        textStyle = TextStyle(
            color = palette.label,
            fontFamily = FontFamily.Monospace,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.14.em,
            // Empty, the cursor sits at the field's start so CodeFieldLayout
            // can put the placeholder right after it; typed, centred.
            textAlign = if (value.isEmpty()) TextAlign.Start else TextAlign.Center,
        ),
        // The legible accent: a yellow cursor would vanish on the light field.
        cursorBrush = SolidColor(palette.accentForeground),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        visualTransformation = UpperCaseTransformation,
        decorationBox = { innerTextField ->
            CodeFieldLayout(
                innerTextField = innerTextField,
                placeholder = if (value.isEmpty()) {
                    {
                        Text(
                            text = AccessGateLabels.PLACEHOLDER,
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                            color = palette.placeholder,
                        )
                    }
                } else {
                    null
                },
            )
        },
    )
}

/**
 * Centres the typed code; while empty, centres "cursor + placeholder" as one
 * group, the way a centred iOS field looks. The empty text field is as wide
 * as its default minimum (about ten characters), so a plain Row or Box would
 * leave the cursor far from the placeholder. The text field stays the first
 * child in every state, so it never loses focus when the placeholder comes
 * and goes.
 */
@Composable
private fun CodeFieldLayout(
    innerTextField: @Composable () -> Unit,
    placeholder: (@Composable () -> Unit)?,
) {
    Layout(
        content = {
            Box { innerTextField() }
            placeholder?.invoke()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ControlHeight)
            .padding(horizontal = 16.dp),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val field = measurables[0].measure(loose)
        val hint = measurables.getOrNull(1)?.measure(loose)
        val width = constraints.maxWidth
        val height = maxOf(constraints.minHeight, field.height, hint?.height ?: 0)
        layout(width, height) {
            if (hint == null) {
                field.place((width - field.width) / 2, (height - field.height) / 2)
            } else {
                val caretWidth = 4.dp.roundToPx()
                val start = ((width - caretWidth - hint.width) / 2).coerceAtLeast(0)
                field.place(start, (height - field.height) / 2)
                hint.place(start + caretWidth, (height - hint.height) / 2)
            }
        }
    }
}

/** Shows the code in capitals without changing what was typed (and so what is submitted). */
private val UpperCaseTransformation = VisualTransformation { text ->
    val upper = text.text.uppercase()
    // A few characters change length when capitalised (ß -> SS); keep those as typed.
    if (upper.length == text.text.length) {
        TransformedText(AnnotatedString(upper), OffsetMapping.Identity)
    } else {
        TransformedText(text, OffsetMapping.Identity)
    }
}

@Composable
private fun InlineError(message: String?, palette: AccessGatePalette) {
    // Holds the last message while the error animates out.
    var shown by remember { mutableStateOf(message) }
    if (message != null) shown = message

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(FADE_IN_MILLIS)) + expandVertically(tween(FADE_IN_MILLIS)),
        exit = fadeOut(tween(FADE_OUT_MILLIS)) + shrinkVertically(tween(FADE_OUT_MILLIS)),
    ) {
        Text(
            text = shown.orEmpty(),
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(AccessGateTestTags.ERROR),
            style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
            color = palette.error,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The accent pill. Disabled and submitting both dim the whole button to 50%,
 * as one layer; while submitting a spinner replaces the label and the button
 * keeps its size.
 */
@Composable
private fun GatePrimaryButton(
    text: String,
    onClick: () -> Unit,
    palette: AccessGatePalette,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val isEnabled = enabled && !isLoading
    Button(
        onClick = onClick,
        enabled = isEnabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = palette.onAccent,
            disabledContainerColor = palette.accent,
            disabledContentColor = palette.onAccent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ControlHeight)
            .alpha(if (isEnabled) 1f else 0.5f),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = palette.onAccent,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text = text,
                style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// region Previews

@OptIn(InternalWandKitApi::class)
private fun previewState(
    phase: WandKitAccessGateViewState.Phase,
    title: String = "Logair beta",
    message: String = "Enter your invite code to continue.",
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) = WandKitAccessGateViewState.HIDDEN.copy(
    phase = phase,
    title = title,
    message = message,
    helpUrl = "https://example.com/request-access",
    errorMessage = errorMessage,
    isSubmitting = isSubmitting,
)

private val PreviewIndigo = Color(0xFF5B5BD6)
private val PreviewGreen = Color(0xFF0E9F6E)
private val PreviewNearBlack = Color(0xFF111827)
private val PreviewYellow = Color(0xFFFFD60A)

@OptIn(InternalWandKitApi::class)
@Composable
private fun AccessGatePreview(
    state: WandKitAccessGateViewState,
    code: String = "",
    dark: Boolean = false,
    accent: Color = PreviewIndigo,
) {
    WandKitThemeProvider(theme = if (dark) WandKitThemeDefaults.dark() else WandKitThemeDefaults.light()) {
        AccessGateView(
            state = state,
            code = code,
            onCodeChange = {},
            onSubmit = {},
            onRetry = {},
            onOpenHelp = {},
            accent = accent,
        )
    }
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateEmptyLightPreview() {
    AccessGatePreview(previewState(WandKitAccessGateViewState.Phase.CodeEntry))
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateTypedLightPreview() {
    AccessGatePreview(previewState(WandKitAccessGateViewState.Phase.CodeEntry), code = "K7QM-2XFT")
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateErrorDarkPreview() {
    AccessGatePreview(
        previewState(WandKitAccessGateViewState.Phase.CodeEntry, errorMessage = "That code isn't valid."),
        code = "WRONG-CODE",
        dark = true,
        accent = PreviewGreen,
    )
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateSubmittingLightPreview() {
    AccessGatePreview(previewState(WandKitAccessGateViewState.Phase.CodeEntry, isSubmitting = true), code = "K7QM-2XFT")
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateCheckingDarkPreview() {
    AccessGatePreview(previewState(WandKitAccessGateViewState.Phase.Checking), dark = true)
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateOfflineLightPreview() {
    AccessGatePreview(previewState(WandKitAccessGateViewState.Phase.Offline), accent = Color(0xFFE5484D))
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateOfflineDarkPreview() {
    AccessGatePreview(previewState(WandKitAccessGateViewState.Phase.Offline), dark = true)
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateLightAccentPreview() {
    AccessGatePreview(previewState(WandKitAccessGateViewState.Phase.CodeEntry), code = "K7QM-2XFT", accent = PreviewYellow)
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateLightAccentDarkPreview() {
    AccessGatePreview(
        previewState(WandKitAccessGateViewState.Phase.CodeEntry),
        code = "K7QM-2XFT",
        dark = true,
        accent = PreviewYellow,
    )
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessGateDarkAccentDarkPreview() {
    AccessGatePreview(
        previewState(WandKitAccessGateViewState.Phase.CodeEntry),
        code = "K7QM-2XFT",
        dark = true,
        accent = PreviewNearBlack,
    )
}

@OptIn(InternalWandKitApi::class)
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun AccessGateLongTextPreview() {
    AccessGatePreview(
        previewState(
            WandKitAccessGateViewState.Phase.CodeEntry,
            title = "Welcome to the Logair private beta programme for early adopters",
            message = "We're letting people in a few at a time. Enter the invite code from your welcome email " +
                "to continue, or request one below and we'll get back to you as soon as there's room.",
            errorMessage = "Too many attempts. Try again in a minute.",
        ),
        code = "K7QM-2XFT",
    )
}

// endregion
