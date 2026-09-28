package com.flabbergast.wandkit.core

import android.app.Application
import android.content.Context
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.platform.PlatformContext
import com.flabbergast.wandkit.core.replay.AndroidSessionReplayRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    configureSessionReplay(config, context.applicationContext)
}

/**
 * Starts the session replay recorder only where a replay could ever be sent:
 * with [WandKitConfig.screenshotReporting] on, on Android 14+ (the only
 * versions with a screenshot signal). Anything else stops it.
 */
private fun configureSessionReplay(config: WandKitConfig, applicationContext: Context) {
    val apply = {
        val container = WandKitSdkContainer.get()
        val options = config.sessionReplay
        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        if (options != null && config.screenshotReporting && supported) {
            AndroidSessionReplayRecorder.start(options, applicationContext)
            container.sessionReplayRecorder = AndroidSessionReplayRecorder
        } else {
            if (options != null) {
                val reason = if (!supported) "below Android 14" else "screenshotReporting is off"
                container.logger.debug("[SessionReplay]", "sessionReplay configured but $reason; a replay can never be sent, not starting the recorder")
            }
            AndroidSessionReplayRecorder.stop()
            container.sessionReplayRecorder = null
        }
    }
    if (Looper.myLooper() == Looper.getMainLooper()) apply() else Handler(Looper.getMainLooper()).post(apply)
}
