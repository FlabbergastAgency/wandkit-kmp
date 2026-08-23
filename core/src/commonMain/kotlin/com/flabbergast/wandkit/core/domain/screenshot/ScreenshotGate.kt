package com.flabbergast.wandkit.core.domain.screenshot

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Decides whether a screenshot should turn into a "Report a problem?" card.
 *
 * Pure so it is testable in `commonTest`; the platform detector gathers the
 * context and asks.
 */
internal object ScreenshotGate {
    internal enum class Skip {
        /** The host never opted in. */
        DISABLED,
        /**
         * Anonymous sessions are read-only: they can neither compose nor
         * upload, and the web app would bounce them to the feed.
         */
        ANONYMOUS,
        /** A survey form or another card is already up. */
        OVERLAY_VISIBLE,
        /**
         * The feedback screen itself is on screen; a screenshot of it is not a
         * bug report about the host app.
         */
        FEEDBACK_VISIBLE,
        /** Backgrounded or in the app switcher. */
        INACTIVE,
        /** Another screenshot within [DEBOUNCE] of the last card. */
        DEBOUNCED,
    }

    internal val DEBOUNCE: Duration = 2.seconds

    internal data class Context(
        val isEnabled: Boolean,
        val isIdentified: Boolean,
        val isOverlayVisible: Boolean,
        val isFeedbackVisible: Boolean,
        val isAppActive: Boolean,
        val lastPromptAt: Instant?,
        val now: Instant,
    )

    /** `null` means "show the card". */
    internal fun skipReason(context: Context): Skip? = when {
        !context.isEnabled -> Skip.DISABLED
        !context.isIdentified -> Skip.ANONYMOUS
        context.isOverlayVisible -> Skip.OVERLAY_VISIBLE
        context.isFeedbackVisible -> Skip.FEEDBACK_VISIBLE
        !context.isAppActive -> Skip.INACTIVE
        context.lastPromptAt != null && context.now - context.lastPromptAt < DEBOUNCE -> Skip.DEBOUNCED
        else -> null
    }
}
