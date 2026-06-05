package com.flabbergast.wandkit.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Immutable
public data class WandKitColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val outline: Color,
)

@Immutable
public data class WandKitTypographyScheme(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

@Immutable
public data class WandKitTheme(
    val colors: WandKitColorScheme,
    val typography: WandKitTypographyScheme,
)

public val LocalWandKitTheme: ProvidableCompositionLocal<WandKitTheme> = staticCompositionLocalOf {
    WandKitThemeDefaults.light()
}

public object WandKitThemeDefaults {
    public fun light(): WandKitTheme = WandKitTheme(
        colors = lightColorScheme().toWandKitColors(),
        typography = Typography().toWandKitTypography(),
    )

    public fun dark(): WandKitTheme = WandKitTheme(
        colors = darkColorScheme().toWandKitColors(),
        typography = Typography().toWandKitTypography(),
    )

    @Composable
    public fun system(
        isDarkTheme: Boolean = isSystemInDarkTheme(),
    ): WandKitTheme = if (isDarkTheme) dark() else light()
}

@Composable
internal fun WandKitThemeProvider(
    theme: WandKitTheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWandKitTheme provides theme,
        content = content,
    )
}

public object WandKitThemeLocal {
    public val current: WandKitTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalWandKitTheme.current
}

public val WandKitColors: WandKitColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalWandKitTheme.current.colors

public val WandKitTypography: WandKitTypographyScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalWandKitTheme.current.typography

private fun ColorScheme.toWandKitColors(): WandKitColorScheme = WandKitColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    outline = outline,
)

private fun Typography.toWandKitTypography(): WandKitTypographyScheme = WandKitTypographyScheme(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
)
