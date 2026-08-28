package com.flabbergast.wandkit.core.screenshot

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the foreground Activity so [com.flabbergast.wandkit.core.feedback.presentFeedbackScreen]
 * can launch on top of it instead of starting a new task, and so
 * [ScreenshotDetector] knows which Activity to arm or disarm as the host app
 * navigates between screens.
 *
 * Registered once, from `WandKit.configureForAndroid`. A [WeakReference]
 * means holding on to a destroyed Activity here can never keep it alive past
 * its own lifecycle.
 */
internal object CurrentActivityTracker : Application.ActivityLifecycleCallbacks {
    private var installed = false
    private var currentActivityRef: WeakReference<Activity>? = null
    private var resumedCount = 0

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
        ScreenshotDetector.onActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        resumedCount = maxOf(0, resumedCount - 1)
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
        ScreenshotDetector.onActivityPaused(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        ScreenshotDetector.onActivityDestroyed(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
