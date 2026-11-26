package com.flabbergast.wandkit.core.review

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.accessgate.AccessGateActivityPresenter
import com.flabbergast.wandkit.core.components.root.WandKitSlotAnimation
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.feedback.WandKitFeedbackActivity
import com.flabbergast.wandkit.core.platform.PlatformContext
import com.flabbergast.wandkit.core.screenshot.CurrentActivityTracker
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

private const val LOGGER_TAG = "[NativeReviewPresenter]"

/**
 * Class of the transparent Activity Play Core launches to host the review
 * card. It belongs to Google, not the host app, so the SDK's screenshot and
 * replay hooks leave it alone.
 */
private const val PLAY_REVIEW_ACTIVITY_CLASS = "com.google.android.play.core.common.PlayCoreDialogWrapperActivity"

internal fun Activity.isPlayReviewActivity(): Boolean = javaClass.name == PLAY_REVIEW_ACTIVITY_CLASS

/**
 * The controller calls in from any thread, so every entry point hops to the
 * main thread before touching [coordinator], which holds all the state.
 */
internal actual object NativeReviewPresenter {
    private val mainHandler = Handler(Looper.getMainLooper())

    @OptIn(InternalWandKitApi::class)
    private val coordinator = NativeReviewCoordinator<ReviewInfo>(
        timer = MainHandlerLaunchTimer(mainHandler),
        exitDelayMillis = WandKitSlotAnimation.EXIT_MILLIS.toLong(),
    )

    actual fun prefetch(platformContext: PlatformContext?, logger: Logger) = onMain {
        coordinator.prefetch(logger) {
            val applicationContext = platformContext?.applicationContext
            if (applicationContext == null) {
                logger.warn(LOGGER_TAG, "No application context; skipping the native review")
                null
            } else {
                PlayReviewFlowClient(ReviewManagerFactory.create(applicationContext))
            }
        }
    }

    actual fun request(logger: Logger) = onMain { coordinator.request(logger) }

    actual fun discard() = onMain { coordinator.discard() }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }
}

private class PlayReviewFlowClient(private val manager: ReviewManager) : ReviewFlowClient<ReviewInfo> {
    override fun requestReviewFlow(onResult: (Result<ReviewInfo>) -> Unit) {
        // Play delivers the result on the main thread.
        manager.requestReviewFlow().addOnCompleteListener { task ->
            onResult(
                if (task.isSuccessful) {
                    Result.success(task.result)
                } else {
                    Result.failure(task.exception ?: IllegalStateException("Review flow request failed"))
                }
            )
        }
    }

    override fun launchReviewFlow(info: ReviewInfo): String? {
        val activity = CurrentActivityTracker.currentActivity ?: return "no foreground Activity"
        val skipReason = when {
            activity.isFinishing || activity.isDestroyed -> "the foreground Activity is going away"
            activity is WandKitFeedbackActivity -> "the WandKit feedback screen is up"
            AccessGateActivityPresenter.isGateActivity(activity) -> "the invite gate is up"
            else -> null
        }
        if (skipReason == null) manager.launchReviewFlow(activity, info)
        return skipReason
    }
}

private class MainHandlerLaunchTimer(private val handler: Handler) : LaunchTimer {
    private var scheduled: Runnable? = null

    override val nowMillis: Long
        get() = SystemClock.uptimeMillis()

    override fun schedule(delayMillis: Long, action: () -> Unit) {
        cancel()
        val runnable = Runnable {
            scheduled = null
            action()
        }
        scheduled = runnable
        handler.postDelayed(runnable, delayMillis)
    }

    override fun cancel() {
        scheduled?.let(handler::removeCallbacks)
        scheduled = null
    }
}
