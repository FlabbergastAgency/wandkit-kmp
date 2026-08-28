package com.flabbergast.wandkit.core.domain.screenshot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class ScreenshotGateTest {
    private val now = Instant.fromEpochMilliseconds(1_000_000)

    /** All-clear by default; each test flips exactly the field(s) it's exercising. */
    private fun context(
        isEnabled: Boolean = true,
        isIdentified: Boolean = true,
        isOverlayVisible: Boolean = false,
        isFeedbackVisible: Boolean = false,
        isAppActive: Boolean = true,
        lastPromptAt: Instant? = null,
        now: Instant = this.now,
    ) = ScreenshotGate.Context(
        isEnabled = isEnabled,
        isIdentified = isIdentified,
        isOverlayVisible = isOverlayVisible,
        isFeedbackVisible = isFeedbackVisible,
        isAppActive = isAppActive,
        lastPromptAt = lastPromptAt,
        now = now,
    )

    @Test
    fun allClearShowsTheCard() {
        assertNull(ScreenshotGate.skipReason(context()))
    }

    @Test
    fun disabledIsSkipped() {
        assertEquals(ScreenshotGate.Skip.DISABLED, ScreenshotGate.skipReason(context(isEnabled = false)))
    }

    @Test
    fun anonymousIsSkipped() {
        assertEquals(ScreenshotGate.Skip.ANONYMOUS, ScreenshotGate.skipReason(context(isIdentified = false)))
    }

    @Test
    fun overlayVisibleIsSkipped() {
        assertEquals(ScreenshotGate.Skip.OVERLAY_VISIBLE, ScreenshotGate.skipReason(context(isOverlayVisible = true)))
    }

    @Test
    fun feedbackVisibleIsSkipped() {
        assertEquals(ScreenshotGate.Skip.FEEDBACK_VISIBLE, ScreenshotGate.skipReason(context(isFeedbackVisible = true)))
    }

    @Test
    fun inactiveIsSkipped() {
        assertEquals(ScreenshotGate.Skip.INACTIVE, ScreenshotGate.skipReason(context(isAppActive = false)))
    }

    @Test
    fun justUnderDebounceWindowIsSkipped() {
        val skip = ScreenshotGate.skipReason(context(lastPromptAt = now - 1999.milliseconds))
        assertEquals(ScreenshotGate.Skip.DEBOUNCED, skip)
    }

    @Test
    fun atDebounceWindowShowsTheCard() {
        val skip = ScreenshotGate.skipReason(context(lastPromptAt = now - 2000.milliseconds))
        assertNull(skip)
    }

    @Test
    fun disabledTakesPrecedenceOverAnonymous() {
        val skip = ScreenshotGate.skipReason(context(isEnabled = false, isIdentified = false))
        assertEquals(ScreenshotGate.Skip.DISABLED, skip)
    }
}
