package com.flabbergast.wandkit.ui.compose.accessGate

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Colour decisions for the gate screen, kept pure so they can be unit tested:
 * which accent to use, what goes on top of it, and when the accent is too
 * close to the background to carry text or a glyph by itself.
 */
internal object AccessGateColors {
    /** The SDK's dark label (RGB 24, 29, 39), used on light accents. */
    val DarkOnAccent: Color = Color(0xFF181D27)

    /** Above this WCAG relative luminance an accent takes dark text instead of white. */
    const val LIGHT_ACCENT_LUMINANCE: Float = 0.45f

    /** Minimum contrast (WCAG, non-text / large text) for the accent as a foreground on the background. */
    const val MIN_FOREGROUND_CONTRAST: Float = 3f

    /** WCAG 2.x relative luminance of an sRGB colour; alpha is ignored. */
    fun relativeLuminance(color: Color): Float {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return (0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)).toFloat()
    }

    /** WCAG contrast ratio between two opaque colours, 1..21. */
    fun contrastRatio(a: Color, b: Color): Float {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /** Label colour on an accent fill: white, or the SDK's dark label on a light accent. */
    fun onAccent(accent: Color): Color =
        if (relativeLuminance(accent) > LIGHT_ACCENT_LUMINANCE) DarkOnAccent else Color.White

    /**
     * The accent as a foreground (link text, icon glyph) on [background]; when
     * it would not reach 3:1 against it - a near-black accent in dark mode, a
     * yellow one in light mode - [fallback] (the on-background colour) instead.
     */
    fun legibleAccent(accent: Color, background: Color, fallback: Color): Color =
        if (contrastRatio(accent, background) < MIN_FOREGROUND_CONTRAST) fallback else accent

    /**
     * Parses the SDK's CSS-style hex colours (`#RRGGBB`, or `#RRGGBBAA`) into
     * an opaque accent. Translucent values are rejected: an accent has to
     * work as a solid button fill. `null` for anything else.
     */
    fun parseOpaqueHex(raw: String?): Color? {
        val hex = raw?.trim() ?: return null
        if (!hex.startsWith("#")) return null
        val digits = hex.substring(1)
        if (digits.length != 6 && digits.length != 8) return null
        if (!digits.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        if (digits.length == 8 && !digits.substring(6).equals("FF", ignoreCase = true)) return null
        val rgb = digits.substring(0, 6).toLong(16)
        return Color(0xFF000000L or rgb)
    }

    /**
     * The gate's accent, first match wins: the accent configured through the
     * SDK, then one taken from the host app's theme, then the SDK theme's
     * default tint.
     */
    fun resolveAccent(sdkAccent: Color?, appThemeAccent: Color?, defaultTint: Color): Color =
        (sdkAccent ?: appThemeAccent ?: defaultTint).copy(alpha = 1f)
}
