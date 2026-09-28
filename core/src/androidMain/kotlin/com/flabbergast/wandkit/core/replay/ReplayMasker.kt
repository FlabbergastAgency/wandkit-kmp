package com.flabbergast.wandkit.core.replay

import android.graphics.Rect
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.config.WandKitSessionReplayOptions
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Contributes extra mask rectangles for UI the View-based walk in
 * [ReplayMasker] can't see into - `ui-compose` registers one that reads the
 * Compose semantics tree, so Compose text fields are masked like `EditText`s.
 */
@InternalWandKitApi
public fun interface WandKitReplayMaskProvider {
    /**
     * Adds rectangles to mask, in [root]'s window coordinates (px), to [into].
     * Called on the main thread once per captured frame; keep it cheap.
     */
    public fun collectMaskRects(
        root: View,
        maskTextInputs: Boolean,
        maskAllText: Boolean,
        into: MutableList<Rect>,
    )
}

/** Registry for [WandKitReplayMaskProvider]s. */
@InternalWandKitApi
public object WandKitReplayMasking {
    private val providers = CopyOnWriteArraySet<WandKitReplayMaskProvider>()

    /** Idempotent. */
    public fun addProvider(provider: WandKitReplayMaskProvider) {
        providers += provider
    }

    public fun removeProvider(provider: WandKitReplayMaskProvider) {
        providers -= provider
    }

    internal fun collect(root: View, options: WandKitSessionReplayOptions, into: MutableList<Rect>) {
        for (provider in providers) {
            runCatching { provider.collectMaskRects(root, options.maskTextInputs, options.maskAllText, into) }
        }
    }
}

/**
 * Finds what to paint over before a frame is encoded - the Android
 * counterpart of the iOS SDK's `WandKitReplayMasker`. Masking is applied to
 * the captured bitmap, never to the live UI.
 *
 * - Always: password inputs.
 * - [WandKitSessionReplayOptions.maskTextInputs] (default on): every `EditText`.
 * - [WandKitSessionReplayOptions.maskWebViews]: `WebView`s.
 * - [WandKitSessionReplayOptions.maskAllText]: every `TextView`.
 * - Host-marked: any view whose `tag` or `contentDescription` contains
 *   `wandkit-mask`.
 * - Plus whatever registered [WandKitReplayMaskProvider]s add (Compose).
 */
@OptIn(InternalWandKitApi::class)
internal object ReplayMasker {
    internal const val MASK_MARKER = "wandkit-mask"

    /** Rectangles in [root]'s window coordinates, px. Main thread only. */
    fun maskRects(root: View, options: WandKitSessionReplayOptions): List<Rect> {
        val rects = mutableListOf<Rect>()
        collect(root, options, rects)
        WandKitReplayMasking.collect(root, options, rects)
        return rects
    }

    private fun collect(view: View, options: WandKitSessionReplayOptions, into: MutableList<Rect>) {
        if (view.visibility != View.VISIBLE || view.alpha <= 0f || view.width <= 0 || view.height <= 0) return

        if (shouldMask(view, options)) {
            // The visible portion only, already clipped by scrolling parents -
            // so a half-scrolled-away field doesn't paint over its neighbours.
            val rect = Rect()
            if (view.getGlobalVisibleRect(rect) && !rect.isEmpty) {
                into += rect
            }
            // Its children are covered by its rect already.
            return
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collect(view.getChildAt(index), options, into)
            }
        }
    }

    private fun shouldMask(view: View, options: WandKitSessionReplayOptions): Boolean {
        println("MYTAG shouldMask entered")
        if (view is TextView && isPassword(view)) return true
        println("MYTAG shouldMask 1")
        if (options.maskTextInputs && view is EditText) return true
        if (options.maskWebViews && view is WebView) return true
        if (options.maskAllText && view is TextView) return true
        if ((view.tag as? CharSequence)?.contains(MASK_MARKER) == true) return true
        println("MYTAG shouldMask 2")
        if (view.contentDescription?.contains(MASK_MARKER) == true) return true
        println("MYTAG shouldMask returned false")
        return false
    }

    private fun isPassword(view: TextView): Boolean {
        if (view.transformationMethod is PasswordTransformationMethod) return true
        val inputType = view.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (inputClass) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }
}
