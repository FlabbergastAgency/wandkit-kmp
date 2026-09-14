package com.flabbergast.wandkit.core.feedback

import com.flabbergast.wandkit.core.di.WandKitSdkContainer

/**
 * Opens the feedback web screen. Android launches `WandKitFeedbackActivity`;
 * the iOS targets of this library have no feedback screen (the native WandKit
 * iOS SDK covers iOS) and log a warning instead.
 *
 * [query] is appended verbatim to the initial URL's query string when set -
 * e.g. the feature-preview flow's one-time follow confirmation.
 */
internal expect fun presentFeedbackScreen(
    container: WandKitSdkContainer,
    screen: WandKitFeedbackScreen,
    query: String? = null,
)
