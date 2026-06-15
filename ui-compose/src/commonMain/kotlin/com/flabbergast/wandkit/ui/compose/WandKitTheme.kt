package com.flabbergast.wandkit.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
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
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    val systemBackground: Color,
    val secondarySystemBackground: Color,
    val tertiarySystemBackground: Color,
    val systemGroupedBackground: Color,
    val secondarySystemGroupedBackground: Color,
    val tertiarySystemGroupedBackground: Color,
    val systemFill: Color,
    val secondarySystemFill: Color,
    val tertiarySystemFill: Color,
    val quaternarySystemFill: Color,
    val placeholderText: Color,
    val separator: Color,
    val opaqueSeparator: Color,
    val link: Color,
    val tintColor: Color,
    val systemGray: Color,
    val gold: Color,
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
        colors = lightWandKitColors(),
        typography = Typography().toWandKitTypography(),
    )

    public fun dark(): WandKitTheme = WandKitTheme(
        colors = darkWandKitColors(),
        typography = Typography().toWandKitTypography(),
    )

    @Composable
    public fun system(
        isDarkTheme: Boolean = isSystemInDarkTheme(),
    ): WandKitTheme = if (isDarkTheme) dark() else light()
}

@Composable
internal fun WandKitThemeProvider(
    theme: WandKitTheme = WandKitThemeDefaults.system(),
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

private fun lightWandKitColors(): WandKitColorScheme = WandKitColorScheme(
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4C3C3C43),
    quaternaryLabel = Color(0x2E3C3C43),
    systemBackground = Color(0xFFFFFFFF),
    secondarySystemBackground = Color(0xFFF2F2F7),
    tertiarySystemBackground = Color(0xFFFFFFFF),
    systemGroupedBackground = Color(0xFFF2F2F7),
    secondarySystemGroupedBackground = Color(0xFFFFFFFF),
    tertiarySystemGroupedBackground = Color(0xFFF2F2F7),
    systemFill = Color(0x33787880),
    secondarySystemFill = Color(0x29787880),
    tertiarySystemFill = Color(0x1F767680),
    quaternarySystemFill = Color(0x14747480),
    placeholderText = Color(0x4C3C3C43),
    separator = Color(0x493C3C43),
    opaqueSeparator = Color(0xFFC6C6C8),
    link = Color(0xFF007AFF),
    tintColor = Color(0xFF007AFF),
    systemGray = Color(0xFFF2F2F7),
    gold = Color(0xFFFFC300),
)

private fun darkWandKitColors(): WandKitColorScheme = WandKitColorScheme(
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4CEBEBF5),
    quaternaryLabel = Color(0x2EEBEBF5),
    systemBackground = Color(0xFF000000),
    secondarySystemBackground = Color(0xFF1C1C1E),
    tertiarySystemBackground = Color(0xFF2C2C2E),
    systemGroupedBackground = Color(0xFF000000),
    secondarySystemGroupedBackground = Color(0xFF1C1C1E),
    tertiarySystemGroupedBackground = Color(0xFF2C2C2E),
    systemFill = Color(0x5C787880),
    secondarySystemFill = Color(0x52787880),
    tertiarySystemFill = Color(0x3D767680),
    quaternarySystemFill = Color(0x2E747480),
    placeholderText = Color(0x4CEBEBF5),
    separator = Color(0xA6545458),
    opaqueSeparator = Color(0xFF38383A),
    link = Color(0xFF0984FF),
    tintColor = Color(0xFF0A84FF),
    systemGray = Color(0xFFF2F2F7),
    gold = Color(0xFFFFC300),
)
