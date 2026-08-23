package com.flabbergast.wandkit.core.feedback

import android.content.Context
import android.content.Intent
import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.screenshot.CurrentActivityTracker
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TAG = "[FeedbackPresenter]"

/**
 * Launches [WandKitFeedbackActivity] on top of whatever the host currently
 * shows.
 *
 * Prefers the foreground Activity (via [CurrentActivityTracker]) so the new
 * Activity lands in the host's own task - its up affordance and the system
 * back gesture return to the host normally. Falls back to the application
 * context with `FLAG_ACTIVITY_NEW_TASK` for a caller invoked before any
 * Activity has resumed (e.g. very early in `Application.onCreate`, or a
 * background push handler), at the cost of starting a separate task.
 */
internal actual fun presentFeedbackScreen(
    container: WandKitSdkContainer,
    screen: WandKitFeedbackScreen,
) {
    val launchId = FeedbackLaunchStore.put(screen)
    val activityContext = CurrentActivityTracker.currentActivity
    val context: Context = activityContext
        ?: container.platformContext?.applicationContext
        ?: run {
            container.logger.warn(TAG, "No Activity and no application context available; cannot present feedback")
            return
        }

    val intent = Intent(context, WandKitFeedbackActivity::class.java)
        .putExtra(WandKitFeedbackActivity.EXTRA_LAUNCH_ID, launchId)
    if (activityContext == null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * Builds the [Intent] that opens the feedback screen, for hosts that want to
 * launch it themselves - from a notification's `PendingIntent`, a deep link
 * handler, or anywhere else that already holds a [Context] - rather than
 * through [WandKit.presentFeedback].
 */
public fun WandKit.feedbackIntent(
    context: Context,
    startAt: WandKitFeedbackScreen = WandKitFeedbackScreen.Feed,
): Intent {
    val launchId = FeedbackLaunchStore.put(startAt)
    return Intent(context, WandKitFeedbackActivity::class.java)
        .putExtra(WandKitFeedbackActivity.EXTRA_LAUNCH_ID, launchId)
}

/**
 * One-shot, in-process hand-off from [presentFeedbackScreen] to
 * [WandKitFeedbackActivity] for the screen it should open on.
 *
 * Intent extras travel through Binder, which caps a single transaction
 * around 1 MB across everything in flight for the process; a
 * [WandKitComposerPrefill] can carry image attachments well past that on its
 * own, so putting the [WandKitFeedbackScreen] straight into the intent would
 * risk a `TransactionTooLargeException`. Keeping it in memory and passing
 * only a UUID sidesteps the limit entirely.
 *
 * If the process dies before the Activity reads the entry back (e.g. it is
 * recreated after being killed in the background), [take] returns `null` for
 * the now-stale id and the Activity falls back to [WandKitFeedbackScreen.Feed]
 * - the same graceful degradation a cold deep link into the feed would give.
 */
internal object FeedbackLaunchStore {
    private val screens = mutableMapOf<String, WandKitFeedbackScreen>()

    @OptIn(ExperimentalUuidApi::class)
    internal fun put(screen: WandKitFeedbackScreen): String {
        val id = Uuid.generateV4().toString()
        synchronized(screens) { screens[id] = screen }
        return id
    }

    /** Removes and returns the entry: it is only ever meant to be read once. */
    internal fun take(id: String?): WandKitFeedbackScreen? {
        if (id == null) return null
        return synchronized(screens) { screens.remove(id) }
    }
}
