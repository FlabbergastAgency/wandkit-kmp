package com.flabbergast.wandkit.core.config

public data class WandKitConfig(
    val apiKey: String,
    val isDebugLoggingEnabled: Boolean,
    /**
     * Overrides the API host (events, forms, referrals, feedback sessions).
     * `null` uses the production API host. For pointing a build at a staging
     * or local backend.
     */
    val apiBaseUrl: String? = null,
    /**
     * Origin the feedback web app is served from. Override to point a build at
     * a staging deployment.
     */
    val feedbackWebUrl: String = "https://sdk.wandkit.app",
    /**
     * Styling for the feedback (posts) UI, which is a hosted web app rather
     * than native views and therefore has a theme of its own. `null` leaves
     * it rendering with its own defaults.
     */
    val feedbackTheme: WandKitFeedbackTheme? = null,
    /**
     * Android 14+ only. When `true`, taking a screenshot shows a "Report a
     * problem?" card (through [WandKitHost]); tapping it opens the feedback
     * composer with that screenshot attached and "Report a problem" selected.
     * The image is read back from the app's own window - nothing is read from
     * Photos, so no runtime permission prompt - and it is only uploaded if the
     * user taps through and posts. Needs an identified user: anonymous sessions
     * cannot post, so they are skipped silently. Off by default.
     */
    val screenshotReporting: Boolean = false,
)
