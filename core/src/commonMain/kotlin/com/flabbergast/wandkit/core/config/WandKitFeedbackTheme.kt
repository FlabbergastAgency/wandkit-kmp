package com.flabbergast.wandkit.core.config

/**
 * Colors and shape the feedback web app adopts, serialized into the webview
 * and applied as CSS custom properties. Colors are CSS hex strings
 * (`#RRGGBB`, or `#RRGGBBAA` when translucent); the native chrome around the
 * webview - window background, spinner - only honours the opaque form.
 */
public data class WandKitFeedbackTheme(
    val primaryColor: String? = null,
    val backgroundColor: String? = null,
    val cornerRadius: Double? = null,
    /**
     * A CSS font family the webview can resolve - a web-safe stack, or a font
     * the hosted app bundles. A font that only exists inside your app will
     * not resolve.
     */
    val fontFamily: String? = null,
    /**
     * [WandKitColorSchemePreference.LIGHT] and [WandKitColorSchemePreference.DARK]
     * also override the native chrome around the webview.
     */
    val preferredColorScheme: WandKitColorSchemePreference = WandKitColorSchemePreference.SYSTEM,
)

public enum class WandKitColorSchemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}
