package com.flabbergast.wandkit.core.screenshot

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.screenshot.ScreenshotGate
import com.flabbergast.wandkit.core.domain.screenshot.ScreenshotPrompt
import com.flabbergast.wandkit.core.feedback.WandKitFeedbackActivity
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Instant

private const val TAG = "[ScreenshotDetector]"

/** Long edge of the buffer PixelCopy captures into, in px - see [ScreenshotEncoder] for why 2000. */
private const val CAPTURE_MAX_LONG_EDGE = 2000

/**
 * Watches for `Activity.registerScreenCaptureCallback` - the only system
 * signal Android has for "the user just took a screenshot" - and turns one
 * into a [ScreenshotPrompt] when [ScreenshotGate] says it should.
 *
 * That callback was added in Android 14 (API 34); earlier versions have no
 * equivalent, so screenshot reporting is simply unavailable below it,
 * regardless of [setEnabled]. One callback is registered per resumed
 * Activity (never [WandKitFeedbackActivity] itself - a screenshot of the
 * feedback UI is not a bug report about the host app) and torn down on
 * pause or destroy, since the callback is scoped to a live window.
 */
internal object ScreenshotDetector {
    private var enabled = false
    private val callbacks = mutableMapOf<Activity, Activity.ScreenCaptureCallback>()
    private var lastPromptAt: Instant? = null

    internal fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            callbacks.keys.toList().forEach { unregister(it) }
        }
    }

    internal fun onActivityResumed(activity: Activity) {
        if (Build.VERSION.SDK_INT < 34 || !enabled) return
        if (activity is WandKitFeedbackActivity) return
        if (callbacks.containsKey(activity)) return

        val callback = Activity.ScreenCaptureCallback { onScreenCaptured(activity) }
        // SecurityException when the host app stripped DETECT_SCREEN_CAPTURE
        // from its merged manifest; a missing surface can also throw here.
        val registered = runCatching {
            activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
        }.isSuccess
        if (registered) {
            callbacks[activity] = callback
        }
    }

    internal fun onActivityPaused(activity: Activity) {
        unregister(activity)
    }

    internal fun onActivityDestroyed(activity: Activity) {
        unregister(activity)
    }

    private fun unregister(activity: Activity) {
        val callback = callbacks.remove(activity) ?: return
        if (Build.VERSION.SDK_INT >= 34) {
            // Throws if the Activity's window is already gone; either way the entry is already removed above.
            runCatching { activity.unregisterScreenCaptureCallback(callback) }
        }
    }

    private fun onScreenCaptured(activity: Activity) {
        val container = runCatching { WandKitSdkContainer.get() }.getOrNull() ?: return
        val now = Clock.System.now()
        val context = ScreenshotGate.Context(
            isEnabled = enabled,
            isIdentified = !container.externalUserId.isNullOrBlank(),
            isOverlayVisible = container.feedbackFormController.form.value != null ||
                container.screenshotPromptController.prompt.value != null,
            isFeedbackVisible = WandKitFeedbackActivity.visibleCount > 0,
            isAppActive = CurrentActivityTracker.isAppActive,
            lastPromptAt = lastPromptAt,
            now = now,
        )

        val skip = ScreenshotGate.skipReason(context)
        if (skip != null) {
            container.logger.debug(TAG, "Screenshot prompt skipped: $skip")
            return
        }

        capture(activity, container, now)
    }

    private fun capture(activity: Activity, container: WandKitSdkContainer, capturedAt: Instant) {
        val decorView = activity.window.peekDecorView()
        if (decorView == null || decorView.width <= 0 || decorView.height <= 0) {
            container.logger.debug(TAG, "Skipping screenshot capture: no laid-out decor view yet")
            return
        }

        val (width, height) = targetSize(decorView.width, decorView.height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // PixelCopy scales the window's content into whatever size `bitmap`
        // already is, so pre-sizing it to the capped dimensions avoids ever
        // allocating a full-resolution (often ~10 MB) buffer just to
        // downscale it a moment later.
        val started = runCatching {
            PixelCopy.request(
                activity.window,
                bitmap,
                { result -> onPixelCopyResult(result, bitmap, capturedAt, container) },
                Handler(Looper.getMainLooper()),
            )
        }.isSuccess

        if (!started) {
            bitmap.recycle()
            container.logger.debug(TAG, "PixelCopy request failed to start (no surface yet)")
        }
    }

    private fun onPixelCopyResult(
        result: Int,
        bitmap: Bitmap,
        capturedAt: Instant,
        container: WandKitSdkContainer,
    ) {
        if (result != PixelCopy.SUCCESS) {
            bitmap.recycle()
            container.logger.debug(TAG, "PixelCopy failed with result code $result")
            return
        }

        lastPromptAt = capturedAt
        container.fireAndForgetTask {
            val attachment = ScreenshotEncoder.jpegAttachment(bitmap, "screenshot.jpg")
            bitmap.recycle()
            if (attachment == null) {
                container.logger.debug(TAG, "Screenshot encoding failed or exceeded the size cap")
            } else {
                container.screenshotPromptController.publish(ScreenshotPrompt(attachment))
            }
        }
    }

    private fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        val longEdge = max(width, height)
        if (longEdge <= CAPTURE_MAX_LONG_EDGE) return width to height

        val scale = CAPTURE_MAX_LONG_EDGE.toDouble() / longEdge
        return max(1, (width * scale).toInt()) to max(1, (height * scale).toInt())
    }
}
