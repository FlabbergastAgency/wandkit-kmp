package com.flabbergast.wandkit.core.replay

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import com.flabbergast.wandkit.core.config.WandKitSessionReplayOptions
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.feedback.WandKitFeedbackActivity
import com.flabbergast.wandkit.core.screenshot.CurrentActivityTracker
import java.io.ByteArrayOutputStream
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "[SessionReplay]"

/**
 * The Android session replay recorder - a port of the iOS SDK's
 * `WandKitReplayRecorder`, writing the same `wandkit-replay` v1 file.
 *
 * - **Frames:** `PixelCopy` of the foreground Activity's window straight into
 *   a bitmap already sized to a 720 px long edge, masked (see
 *   [ReplayMasker]) with a placeholder painted where the soft keyboard is,
 *   deduped by a 16x16 grid hash, then JPEG-encoded (quality 40) and stored on
 *   [ReplayIo]. Triggered by touch-down/up (at most one per 250 ms) and a 1 Hz
 *   timer, dropping to 0.5 Hz while captures cost more than 30 ms of main
 *   thread; a keyframe is kept every 30 s even on a static screen.
 * - **Touches:** a passive [ReplayWindowCallback] per Activity.
 * - **Events:** `WandKit.event(...)` calls, via [recordEvent].
 * - **Pauses** (with a `gap` event on resume) while the app is not in the
 *   foreground, while SDK UI is up (feedback screen, screenshot card, survey,
 *   feature-preview sheet), and after three failed captures in a row
 *   (retried on the next Activity resume). **Wipes** the buffer when the app
 *   goes to the background.
 *
 * Every mutable field is confined to the main thread; only [status] and
 * [recordEvent] may be called from elsewhere.
 */
internal object AndroidSessionReplayRecorder : SessionReplayRecorder {
    private const val TOUCH_FRAME_THROTTLE_MILLIS = 250L
    private const val SLOW_CAPTURE_THRESHOLD_MILLIS = 30.0
    private const val NORMAL_INTERVAL_MILLIS = 1_000L
    private const val SLOW_INTERVAL_MILLIS = 2_000L
    private const val RECOVERY_WINDOW_MILLIS = 60_000L
    private const val BLANK_CAPTURE_PAUSE_THRESHOLD = 3
    private const val KEYFRAME_INTERVAL_MILLIS = 30_000L
    private const val FRAME_MAX_LONG_EDGE = 720
    private const val JPEG_QUALITY = 40
    private const val GRID_SIDE = 16
    private const val MASK_CORNER_RADIUS_DP = 8f
    private const val MASK_COLOR = 0xFF333333.toInt()

    private class Geometry(val widthDp: Int, val heightDp: Int, val density: Float)

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var options = WandKitSessionReplayOptions()
    private var buffer: ReplayBuffer? = null
    private val deduper = ReplayFrameDeduper()
    private val moveThrottle = ReplayTouchMoveThrottle()
    private val touchCallbackInstalled = WeakHashMap<Activity, Boolean>()

    private var isRecording = false
    private var isPersistent = false

    /** Bumped whenever in-flight captures must be dropped: stop, freeze, background wipe. */
    private var generation = 0

    private var isPaused = false
    private var pauseStartedAt: Long? = null
    private var consecutiveBlankCaptures = 0
    private var blankPauseActive = false

    private var isReducedRate = false
    private var underThresholdSince: Long? = null
    private var lastKeptFrameAt: Long? = null
    private var lastFrameAttemptAt: Long? = null
    private var captureInFlight = false
    private var tickScheduled = false
    private var lastGeometry: Geometry? = null

    @Volatile
    private var publishedStatus: WandKitSessionReplayStatus? = null

    private val tickRunnable = Runnable {
        tickScheduled = false
        tick()
    }

    override val status: WandKitSessionReplayStatus?
        get() = publishedStatus

    // region Start / stop

    /** Main thread. A no-op when already recording with equal [options]. */
    fun start(options: WandKitSessionReplayOptions, context: Context?) {
        if (isRecording && this.options == options) return
        stop()

        // Sweep leftovers from a previous process even when this run keeps
        // everything in memory - they are ours to clean up either way.
        val disk = context?.let { runCatching { ReplayDiskStorage.forContext(it) }.getOrNull() }
            ?.takeIf { options.persistToDisk }

        this.options = options
        isPersistent = disk != null
        buffer = ReplayBuffer(
            windowSeconds = options.windowSeconds,
            maxBytes = options.maxBytes,
            storeFactory = { disk?.newFrameStore() ?: InMemoryReplayFrameStore() },
        )
        deduper.reset()
        moveThrottle.reset()
        lastKeptFrameAt = null
        lastFrameAttemptAt = null
        consecutiveBlankCaptures = 0
        blankPauseActive = false
        isReducedRate = false
        underThresholdSince = null
        isPaused = false
        pauseStartedAt = null
        captureInFlight = false
        generation += 1
        isRecording = true

        CurrentActivityTracker.currentActivity?.let(::installTouchCallback)
        reevaluatePause()
        scheduleTick()
        publishStatus()
        logger()?.debug(TAG, "Recorder started (persistToDisk=$isPersistent, window=${options.windowSeconds}s, cap=${options.maxBytes}B)")
    }

    /** Main thread. Drops the buffer (and its files). */
    fun stop() {
        if (!isRecording) return
        isRecording = false
        generation += 1
        mainHandler.removeCallbacks(tickRunnable)
        tickScheduled = false
        buffer?.close()
        buffer = null
        isPaused = false
        pauseStartedAt = null
        captureInFlight = false
        publishedStatus = null
        logger()?.debug(TAG, "Recorder stopped")
    }

    // endregion

    // region SessionReplayRecorder

    override fun recordEvent(name: String, properties: Map<String, String>) {
        val at = System.currentTimeMillis()
        runOnMain {
            if (!isRecording) return@runOnMain
            buffer?.append(ReplayEvent.Event.fromProperties(name, properties), at)
            publishStatus()
        }
    }

    override fun freeze(): ReplaySnapshot? {
        check(Looper.myLooper() == Looper.getMainLooper()) { "freeze() must run on the main thread" }
        if (!isRecording) return null
        val currentBuffer = buffer ?: return null
        val header = makeHeader() ?: return null
        val snapshot = currentBuffer.freeze(header) ?: return null

        // Frames still being encoded belong to the snapshot's store now,
        // which is about to be destroyed - drop them.
        generation += 1
        captureInFlight = false
        deduper.reset()
        lastKeptFrameAt = null
        publishStatus()
        return snapshot
    }

    // endregion

    // region Lifecycle (called by CurrentActivityTracker, main thread)

    internal fun onActivityResumed(activity: Activity) {
        if (!isRecording) return
        installTouchCallback(activity)
        if (activity !is WandKitFeedbackActivity) {
            consecutiveBlankCaptures = 0
            blankPauseActive = false
        }
        reevaluatePause()
        scheduleTick()
    }

    internal fun onActivityPaused() {
        if (!isRecording) return
        reevaluatePause()
    }

    /** No Activity started anymore (and not just a configuration change): discard everything. */
    internal fun onAppBackgrounded() {
        if (!isRecording) return
        reevaluatePause()
        buffer?.removeAll()
        generation += 1
        captureInFlight = false
        deduper.reset()
        lastKeptFrameAt = null
        mainHandler.removeCallbacks(tickRunnable)
        tickScheduled = false
        publishStatus()
    }

    // endregion

    // region Timer

    private fun tick() {
        if (!isRecording) return
        reevaluatePause()
        if (!isPaused) triggerFrame(fromTouch = false)
        // Keep ticking while in the foreground, paused or not, so a pause for
        // SDK UI lifts on its own once that UI is gone.
        if (CurrentActivityTracker.isAppActive) scheduleTick()
    }

    private fun scheduleTick() {
        if (!isRecording || tickScheduled) return
        tickScheduled = true
        mainHandler.postDelayed(tickRunnable, if (isReducedRate) SLOW_INTERVAL_MILLIS else NORMAL_INTERVAL_MILLIS)
    }

    private fun rescheduleTick() {
        mainHandler.removeCallbacks(tickRunnable)
        tickScheduled = false
        scheduleTick()
    }

    // endregion

    // region Touches

    private fun installTouchCallback(activity: Activity) {
        if (activity is WandKitFeedbackActivity) return
        if (touchCallbackInstalled[activity] == true) return
        val window = activity.window ?: return
        val current = window.callback ?: return
        if (current !is ReplayWindowCallback) {
            window.callback = ReplayWindowCallback(current) { event -> handleTouch(activity, event) }
        }
        touchCallbackInstalled[activity] = true
    }

    private fun handleTouch(activity: Activity, event: MotionEvent) {
        if (!isRecording || isPaused) return
        val phase = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> ReplayTouchPhase.DOWN
            MotionEvent.ACTION_MOVE -> ReplayTouchPhase.MOVE
            MotionEvent.ACTION_UP -> ReplayTouchPhase.UP
            MotionEvent.ACTION_CANCEL -> ReplayTouchPhase.CANCEL
            else -> return
        }
        val at = System.currentTimeMillis()
        if (phase == ReplayTouchPhase.MOVE && !moveThrottle.shouldReport(at)) return

        val decor = activity.window?.peekDecorView() ?: return
        val location = IntArray(2)
        decor.getLocationOnScreen(location)
        val density = decor.resources.displayMetrics.density
        val x = oneDecimal((event.rawX - location[0]) / density)
        val y = oneDecimal((event.rawY - location[1]) / density)

        buffer?.append(ReplayEvent.Touch(phase, x, y), at)
        publishStatus()

        if (phase == ReplayTouchPhase.DOWN || phase == ReplayTouchPhase.UP) {
            triggerFrame(fromTouch = true)
        }
    }

    // endregion

    // region Frame pipeline

    private fun triggerFrame(fromTouch: Boolean) {
        if (!isRecording || isPaused || captureInFlight) return
        val at = System.currentTimeMillis()
        val last = lastFrameAttemptAt
        if (fromTouch && last != null && at - last < TOUCH_FRAME_THROTTLE_MILLIS) return
        lastFrameAttemptAt = at
        captureFrame(at)
    }

    private fun captureFrame(capturedAt: Long) {
        val activity = CurrentActivityTracker.currentActivity
        if (activity == null || activity is WandKitFeedbackActivity) return

        val window = activity.window
        val decor = window?.peekDecorView()
        if (window == null || decor == null || decor.width <= 0 || decor.height <= 0 ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        ) {
            handleBlankCapture()
            return
        }

        val startedNanos = SystemClock.elapsedRealtimeNanos()
        val decorWidth = decor.width
        val decorHeight = decor.height
        val density = decor.resources.displayMetrics.density
        lastGeometry = Geometry(
            widthDp = (decorWidth / density).roundToInt(),
            heightDp = (decorHeight / density).roundToInt(),
            density = density,
        )

        // Masks and the keyboard rect are read now, on the main thread, from
        // the same layout PixelCopy is about to capture.
        val masks = runCatching { ReplayMasker.maskRects(decor, options) }.getOrDefault(emptyList())
        val keyboard = runCatching { ReplayKeyboardPlaceholder.imeRect(decor) }.getOrNull()
        val dark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val (width, height) = targetSize(decorWidth, decorHeight)
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return
        }

        val captureGeneration = generation
        val preCostMillis = elapsedMillis(startedNanos)
        captureInFlight = true
        val requested = runCatching {
            requestPixelCopy(window, bitmap) { result ->
                onPixelCopyResult(
                    result = result,
                    bitmap = bitmap,
                    captureGeneration = captureGeneration,
                    capturedAt = capturedAt,
                    masks = masks,
                    keyboard = keyboard,
                    sourceWidth = decorWidth,
                    sourceHeight = decorHeight,
                    density = density,
                    dark = dark,
                    preCostMillis = preCostMillis,
                )
            }
        }.isSuccess

        if (!requested) {
            captureInFlight = false
            bitmap.recycle()
            handleBlankCapture()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestPixelCopy(window: Window, bitmap: Bitmap, onResult: (Int) -> Unit) {
        // PixelCopy scales the window into whatever size `bitmap` already
        // is, so no full-resolution buffer is ever allocated.
        PixelCopy.request(window, bitmap, { result -> onResult(result) }, mainHandler)
    }

    private fun onPixelCopyResult(
        result: Int,
        bitmap: Bitmap,
        captureGeneration: Int,
        capturedAt: Long,
        masks: List<Rect>,
        keyboard: Rect?,
        sourceWidth: Int,
        sourceHeight: Int,
        density: Float,
        dark: Boolean,
        preCostMillis: Double,
    ) {
        if (captureGeneration != generation || !isRecording) {
            bitmap.recycle()
            return
        }
        captureInFlight = false
        if (result != PixelCopy.SUCCESS) {
            bitmap.recycle()
            handleBlankCapture()
            return
        }
        consecutiveBlankCaptures = 0

        val startedNanos = SystemClock.elapsedRealtimeNanos()
        paintOverlays(bitmap, masks, keyboard, sourceWidth, sourceHeight, density, dark)
        val hash = gridHash(bitmap)
        adjustCadence(preCostMillis + elapsedMillis(startedNanos))

        // Evaluated first either way, so the deduper always tracks the
        // latest distinct frame.
        val changed = deduper.shouldKeep(hash)
        val keyframeDue = lastKeptFrameAt?.let { capturedAt - it >= KEYFRAME_INTERVAL_MILLIS } ?: true
        if (!changed && !keyframeDue) {
            bitmap.recycle()
            return
        }
        lastKeptFrameAt = capturedAt

        val store = buffer?.store ?: run {
            bitmap.recycle()
            return
        }
        val frameWidth = bitmap.width
        val frameHeight = bitmap.height

        ReplayIo.execute {
            val jpeg = try {
                ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
            } finally {
                bitmap.recycle()
            }
            val ref = store.put(jpeg) ?: return@execute
            mainHandler.post {
                val current = buffer
                if (isRecording && captureGeneration == generation && current != null && current.store === store) {
                    current.append(ReplayEvent.Frame(frameWidth, frameHeight, ref), capturedAt)
                    publishStatus()
                } else {
                    store.delete(ref)
                }
            }
        }
    }

    private fun paintOverlays(
        bitmap: Bitmap,
        masks: List<Rect>,
        keyboard: Rect?,
        sourceWidth: Int,
        sourceHeight: Int,
        density: Float,
        dark: Boolean,
    ) {
        if (masks.isEmpty() && keyboard == null) return
        val canvas = Canvas(bitmap)
        val scaleX = bitmap.width.toFloat() / sourceWidth
        val scaleY = bitmap.height.toFloat() / sourceHeight

        if (masks.isNotEmpty()) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MASK_COLOR }
            for (mask in masks) {
                val rect = RectF(mask.left * scaleX, mask.top * scaleY, mask.right * scaleX, mask.bottom * scaleY)
                val radius = min(MASK_CORNER_RADIUS_DP * density * scaleX, min(rect.width(), rect.height()) / 2f)
                canvas.drawRoundRect(rect, max(0f, radius), max(0f, radius), paint)
            }
        }

        keyboard?.let {
            val rect = RectF(it.left * scaleX, it.top * scaleY, it.right * scaleX, it.bottom * scaleY)
            ReplayKeyboardPlaceholder.paint(canvas, rect, dark)
        }
    }

    private fun gridHash(bitmap: Bitmap): ULong {
        val small = Bitmap.createScaledBitmap(bitmap, GRID_SIDE, GRID_SIDE, true)
        val pixels = IntArray(GRID_SIDE * GRID_SIDE)
        small.getPixels(pixels, 0, GRID_SIDE, 0, 0, GRID_SIDE, GRID_SIDE)
        if (small !== bitmap) small.recycle()

        val grid = ByteArray(pixels.size) { index ->
            val color = pixels[index]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            ((r * 299 + g * 587 + b * 114) / 1000).toByte()
        }
        return ReplayFrameDeduper.hash(grid)
    }

    private fun handleBlankCapture() {
        consecutiveBlankCaptures += 1
        if (consecutiveBlankCaptures < BLANK_CAPTURE_PAUSE_THRESHOLD || blankPauseActive) return
        blankPauseActive = true
        logger()?.debug(TAG, "Pausing after $BLANK_CAPTURE_PAUSE_THRESHOLD failed captures; retrying on the next Activity resume")
        reevaluatePause()
    }

    private fun adjustCadence(elapsedMillis: Double) {
        if (elapsedMillis > SLOW_CAPTURE_THRESHOLD_MILLIS) {
            underThresholdSince = null
            if (!isReducedRate) {
                isReducedRate = true
                rescheduleTick()
            }
            return
        }
        if (!isReducedRate) return

        val checkedAt = System.currentTimeMillis()
        val since = underThresholdSince
        if (since == null) {
            underThresholdSince = checkedAt
            return
        }
        if (checkedAt - since < RECOVERY_WINDOW_MILLIS) return
        isReducedRate = false
        underThresholdSince = null
        rescheduleTick()
    }

    private fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        val longEdge = max(width, height)
        if (longEdge <= FRAME_MAX_LONG_EDGE) return width to height
        val scale = FRAME_MAX_LONG_EDGE.toDouble() / longEdge
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    // endregion

    // region Pause / resume

    private fun reevaluatePause() {
        if (!isRecording) return
        val shouldPause = !CurrentActivityTracker.isAppActive || isSdkUiVisible() || blankPauseActive
        if (shouldPause == isPaused) return
        isPaused = shouldPause

        val at = System.currentTimeMillis()
        if (isPaused) {
            pauseStartedAt = at
        } else {
            val pausedMillis = pauseStartedAt?.let { max(0L, at - it) } ?: 0L
            pauseStartedAt = null
            buffer?.append(ReplayEvent.Gap(pausedMillis), at)
        }
        publishStatus()
    }

    private fun isSdkUiVisible(): Boolean {
        if (WandKitFeedbackActivity.visibleCount > 0) return true
        val container = container() ?: return false
        return container.screenshotPromptController.prompt.value != null ||
            container.feedbackFormController.form.value != null ||
            container.featurePreviewController.prompt.value != null
    }

    // endregion

    // region Helpers

    private fun makeHeader(): ReplayHeader? {
        val geometry = lastGeometry ?: return null
        val container = container() ?: return null
        return ReplayHeader(
            // Replaced by ReplayBuffer.freeze with the oldest retained frame's time.
            startEpochMillis = 0L,
            width = geometry.widthDp,
            height = geometry.heightDp,
            scale = geometry.density.toDouble(),
            device = container.replayDevice(),
            sdk = container.replaySdkName,
        )
    }

    private fun publishStatus() {
        val currentBuffer = buffer
        publishedStatus = if (!isRecording || currentBuffer == null) {
            null
        } else {
            WandKitSessionReplayStatus(
                frameCount = currentBuffer.frameCount,
                bufferedBytes = currentBuffer.byteCount,
                isPaused = isPaused,
                isPersistedToDisk = isPersistent,
            )
        }
    }

    private fun container(): WandKitSdkContainer? = runCatching { WandKitSdkContainer.get() }.getOrNull()

    private fun logger() = container()?.logger

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun elapsedMillis(sinceNanos: Long): Double = (SystemClock.elapsedRealtimeNanos() - sinceNanos) / 1_000_000.0

    private fun oneDecimal(value: Float): Double = (value * 10f).roundToInt() / 10.0

    // endregion
}
