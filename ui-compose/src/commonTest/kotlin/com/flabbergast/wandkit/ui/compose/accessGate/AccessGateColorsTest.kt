package com.flabbergast.wandkit.ui.compose.accessGate

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccessGateColorsTest {
    private val white = Color(0xFFFFFFFF)
    private val black = Color(0xFF000000)
    private val darkLabel = Color(0xFF181D27)

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")
    }

    @Test
    fun relativeLuminance_matchesWcagReferenceValues() {
        assertClose(1f, AccessGateColors.relativeLuminance(white), 0.0001f)
        assertClose(0f, AccessGateColors.relativeLuminance(black), 0.0001f)
        // sRGB #777777 is ~0.184.
        assertClose(0.184f, AccessGateColors.relativeLuminance(Color(0xFF777777)))
        // Pure primaries weigh in by their coefficients.
        assertClose(0.2126f, AccessGateColors.relativeLuminance(Color(0xFFFF0000)), 0.001f)
        assertClose(0.7152f, AccessGateColors.relativeLuminance(Color(0xFF00FF00)), 0.001f)
        assertClose(0.0722f, AccessGateColors.relativeLuminance(Color(0xFF0000FF)), 0.001f)
    }

    @Test
    fun contrastRatio_isSymmetricAndSpansOneToTwentyOne() {
        assertClose(21f, AccessGateColors.contrastRatio(white, black))
        assertClose(21f, AccessGateColors.contrastRatio(black, white))
        assertClose(1f, AccessGateColors.contrastRatio(Color(0xFF5B5BD6), Color(0xFF5B5BD6)))
    }

    @Test
    fun onAccent_isWhiteOnDarkOrSaturatedAccents() {
        listOf(0xFF5B5BD6, 0xFF007AFF, 0xFF0E9F6E, 0xFFE5484D, 0xFFF76B15, 0xFF111827).forEach {
            assertEquals(Color.White, AccessGateColors.onAccent(Color(it)), "accent ${it.toString(16)}")
        }
    }

    @Test
    fun onAccent_isTheDarkLabelOnLightAccents() {
        listOf(0xFFFFD60A, 0xFFFFFFFF, 0xFF7CFFB2).forEach {
            assertEquals(darkLabel, AccessGateColors.onAccent(Color(it)), "accent ${it.toString(16)}")
        }
        assertEquals(darkLabel, AccessGateColors.DarkOnAccent)
    }

    @Test
    fun onAccent_switchesJustAboveTheThreshold() {
        // #BBBBBB is ~0.497, #AAAAAA ~0.402.
        assertEquals(darkLabel, AccessGateColors.onAccent(Color(0xFFBBBBBB)))
        assertEquals(Color.White, AccessGateColors.onAccent(Color(0xFFAAAAAA)))
    }

    @Test
    fun legibleAccent_keepsAnAccentWithEnoughContrast() {
        val indigo = Color(0xFF5B5BD6)
        assertEquals(indigo, AccessGateColors.legibleAccent(indigo, background = white, fallback = darkLabel))
        val green = Color(0xFF0E9F6E)
        assertEquals(green, AccessGateColors.legibleAccent(green, background = black, fallback = white))
    }

    @Test
    fun legibleAccent_fallsBackForExtremeAccents() {
        // Near-black on black and yellow on white are both well under 3:1.
        assertEquals(white, AccessGateColors.legibleAccent(Color(0xFF111827), background = black, fallback = white))
        assertEquals(darkLabel, AccessGateColors.legibleAccent(Color(0xFFFFD60A), background = white, fallback = darkLabel))
        // ...while each is fine on the opposite background.
        assertEquals(Color(0xFF111827), AccessGateColors.legibleAccent(Color(0xFF111827), background = white, fallback = darkLabel))
        assertEquals(Color(0xFFFFD60A), AccessGateColors.legibleAccent(Color(0xFFFFD60A), background = black, fallback = white))
    }

    @Test
    fun parseOpaqueHex_acceptsSixAndOpaqueEightDigitForms() {
        assertEquals(Color(0xFF0E9F6E), AccessGateColors.parseOpaqueHex("#0E9F6E"))
        assertEquals(Color(0xFF0E9F6E), AccessGateColors.parseOpaqueHex("#0e9f6e"))
        assertEquals(Color(0xFF0E9F6E), AccessGateColors.parseOpaqueHex(" #0E9F6EFF "))
    }

    @Test
    fun parseOpaqueHex_rejectsEverythingElse() {
        assertNull(AccessGateColors.parseOpaqueHex(null))
        assertNull(AccessGateColors.parseOpaqueHex(""))
        assertNull(AccessGateColors.parseOpaqueHex("0E9F6E"))
        assertNull(AccessGateColors.parseOpaqueHex("#FFF"))
        assertNull(AccessGateColors.parseOpaqueHex("#0E9F6E80"))
        assertNull(AccessGateColors.parseOpaqueHex("#GG9F6E"))
        assertNull(AccessGateColors.parseOpaqueHex("rgb(1,2,3)"))
    }

    @Test
    fun resolveAccent_prefersTheSdkAccentThenTheAppThemeThenTheTint() {
        val sdk = Color(0xFF0E9F6E)
        val app = Color(0xFFE5484D)
        val tint = Color(0xFF007AFF)
        assertEquals(sdk, AccessGateColors.resolveAccent(sdk, app, tint))
        assertEquals(app, AccessGateColors.resolveAccent(null, app, tint))
        assertEquals(tint, AccessGateColors.resolveAccent(null, null, tint))
        // Always opaque, whatever it came from.
        assertEquals(1f, AccessGateColors.resolveAccent(null, app.copy(alpha = 0.4f), tint).alpha)
    }
}
