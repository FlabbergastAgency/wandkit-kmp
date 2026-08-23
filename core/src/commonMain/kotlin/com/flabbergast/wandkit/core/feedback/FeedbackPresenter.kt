package com.flabbergast.wandkit.core.feedback

import com.flabbergast.wandkit.core.di.WandKitSdkContainer

/**
 * Opens the feedback web screen. Android launches `WandKitFeedbackActivity`;
 * the iOS targets of this library have no feedback screen (the native WandKit
 * iOS SDK covers iOS) and log a warning instead.
 */
internal expect fun presentFeedbackScreen(
    container: WandKitSdkContainer,
    screen: WandKitFeedbackScreen,
)
