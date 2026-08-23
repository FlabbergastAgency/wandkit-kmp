package com.flabbergast.wandkit.core

import android.app.Application
import android.content.Context
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.platform.PlatformContext
import com.flabbergast.wandkit.core.screenshot.CurrentActivityTracker
import com.flabbergast.wandkit.core.screenshot.ScreenshotDetector

public fun WandKit.configure(
    config: WandKitConfig,
    context: Context,
) {
    configureForAndroid(config, context)
}

/**
 * Configures the SDK on Android.
 *
 * Call from `Application.onCreate` rather than an Activity's: this is where
 * [CurrentActivityTracker] registers its `ActivityLifecycleCallbacks`, and
 * configuring any later means it misses whichever Activity is already
 * resumed by the time it runs - leaving [com.flabbergast.wandkit.core.feedback.presentFeedbackScreen]
 * without a foreground Activity to launch on top of, and [ScreenshotDetector]
 * with nothing armed, until the next Activity starts.
 */
public fun WandKit.configureForAndroid(
    config: WandKitConfig,
    context: Context,
) {
    WandKitSdkContainer.init(config, PlatformContext(context.applicationContext))
    (context.applicationContext as? Application)?.let { CurrentActivityTracker.install(it) }
    ScreenshotDetector.setEnabled(config.screenshotReporting)
}
