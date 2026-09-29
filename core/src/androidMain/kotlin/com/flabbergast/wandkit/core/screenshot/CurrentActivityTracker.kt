package com.flabbergast.wandkit.core.screenshot

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.flabbergast.wandkit.core.accessgate.AccessGateActivityPresenter
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.replay.AndroidSessionReplayRecorder
import java.lang.ref.WeakReference

/**
 * Tracks the foreground Activity so [com.flabbergast.wandkit.core.feedback.presentFeedbackScreen]
 * can launch on top of it instead of starting a new task, and so
 * [ScreenshotDetector] knows which Activity to arm or disarm as the host app
 * navigates between screens, and so the invite gate can cover every host
 * Activity ([AccessGateActivityPresenter]) and re-check a cached pass when the
 * app returns to the foreground.
 *
 * Registered once, from `WandKit.configureForAndroid`. A [WeakReference]
 * means holding on to a destroyed Activity here can never keep it alive past
 * its own lifecycle.
 */
internal object CurrentActivityTracker : Application.ActivityLifecycleCallbacks {
    private var installed = false
    private var currentActivityRef: WeakReference<Activity>? = null
    private var resumedCount = 0
    private var startedCount = 0

    /**
     * Whether the last time the started count dropped to zero was a
     * configuration change (rotation, dark mode) rather than the app leaving
     * the foreground - the restart that follows is then not a foreground.
     */
    private var lastStopWasConfigurationChange = false

    internal val currentActivity: Activity?
        get() = currentActivityRef?.get()

    /** Whether at least one Activity is currently resumed - i.e. the app is in the foreground. */
    internal val isAppActive: Boolean
        get() = resumedCount > 0

    /** Safe to call more than once: a second `configure()` must not double-register the callbacks. */
    internal fun install(application: Application) {
        if (installed) return
        installed = true
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        resumedCount++
        AccessGateActivityPresenter.onActivityResumed(activity)
        ScreenshotDetector.onActivityResumed(activity)
        AndroidSessionReplayRecorder.onActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        resumedCount = maxOf(0, resumedCount - 1)
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
        ScreenshotDetector.onActivityPaused(activity)
        AndroidSessionReplayRecorder.onActivityPaused()
    }

    override fun onActivityDestroyed(activity: Activity) {
        ScreenshotDetector.onActivityDestroyed(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        AccessGateActivityPresenter.onActivityCreated(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        val isForeground = startedCount == 0 && !lastStopWasConfigurationChange
        startedCount++
        lastStopWasConfigurationChange = false
        if (isForeground) {
            // The first start after configure counts too; the controller's
            // throttle treats the launch check as the most recent one.
            WandKitSdkContainer.activeAccessGate.value?.onAppForegrounded()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount = maxOf(0, startedCount - 1)
        // A rotation stops the old Activity before starting the new one; that
        // dip to zero is not the app going to the background.
        if (startedCount == 0) {
            lastStopWasConfigurationChange = activity.isChangingConfigurations
        }
        if (startedCount == 0 && !activity.isChangingConfigurations) {
            AndroidSessionReplayRecorder.onAppBackgrounded()
        }
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
