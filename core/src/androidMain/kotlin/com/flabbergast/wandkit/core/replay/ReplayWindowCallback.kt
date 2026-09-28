package com.flabbergast.wandkit.core.replay

import android.view.MotionEvent
import android.view.Window

/**
 * Observes touches for the replay timeline by wrapping an Activity's
 * [Window.Callback] - the Android counterpart of the passive gesture
 * recognizer the iOS SDK adds to each window. Purely passive: every call is
 * forwarded to [delegate] unchanged, and the observer can never consume or
 * alter an event (its failures are swallowed).
 *
 * Installed once per Activity by the recorder and left in place; when the
 * recorder stops, [onTouch] just returns without recording.
 */
internal class ReplayWindowCallback(
    internal val delegate: Window.Callback,
    private val onTouch: (MotionEvent) -> Unit,
) : Window.Callback by delegate {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            runCatching { onTouch(event) }
        }
        return delegate.dispatchTouchEvent(event)
    }
}

/** Throttles `move` touches the way the iOS recognizer does (one per 50 ms). */
internal class ReplayTouchMoveThrottle(private val intervalMillis: Long = 50) {
    private var lastReportedAt: Long? = null

    fun shouldReport(atMillis: Long): Boolean {
        val last = lastReportedAt
        if (last != null && atMillis - last < intervalMillis) return false
        lastReportedAt = atMillis
        return true
    }

    fun reset() {
        lastReportedAt = null
    }
}
