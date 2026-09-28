package com.flabbergast.wandkit.core.replay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * The soft keyboard is its own window, drawn by another process, so
 * `PixelCopy` of the app's window never contains it - frames would show the
 * app's content sliding up over empty space. Like the iOS SDK, the recorder
 * paints a stylised placeholder keyboard where the IME is, before the frame is
 * hashed, deduped and encoded (so a keyboard appearing counts as a change).
 * It never shows what was typed.
 */
internal object ReplayKeyboardPlaceholder {
    /** Where the IME covers [decorView], in window px; `null` when it is hidden. */
    fun imeRect(decorView: View): Rect? {
        val insets = ViewCompat.getRootWindowInsets(decorView) ?: return null
        if (!insets.isVisible(WindowInsetsCompat.Type.ime())) return null
        val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        if (bottom <= 0) return null
        return Rect(0, decorView.height - bottom, decorView.width, decorView.height)
    }

    /** Paints a placeholder keyboard filling [rect] (bitmap coordinates). */
    fun paint(canvas: Canvas, rect: RectF, dark: Boolean) {
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (dark) 0xFF2B2B2D.toInt() else 0xFFD1D3D9.toInt() }
        val key = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (dark) 0xFF6B6B6E.toInt() else 0xFFFFFFFF.toInt() }
        canvas.drawRect(rect, background)

        // Four rows: 10 letter keys, 9, 7 between two wide keys, then a
        // space bar between two wide keys.
        val rows = listOf(
            List(10) { 1f },
            List(9) { 1f },
            listOf(1.5f) + List(7) { 1f } + listOf(1.5f),
            listOf(2.5f, 5f, 2.5f),
        )
        val padding = rect.height() * 0.04f
        val gap = rect.width() * 0.012f
        val rowHeight = (rect.height() - padding * 2 - gap * (rows.size - 1)) / rows.size
        val radius = rowHeight * 0.12f

        rows.forEachIndexed { rowIndex, weights ->
            val top = rect.top + padding + rowIndex * (rowHeight + gap)
            val unit = (rect.width() - padding * 2 - gap * 9) / 10f
            val rowWidth = weights.sum() * unit + gap * (weights.size - 1)
            var left = rect.left + (rect.width() - rowWidth) / 2f
            for (weight in weights) {
                val width = weight * unit
                canvas.drawRoundRect(RectF(left, top, left + width, top + rowHeight), radius, radius, key)
                left += width + gap
            }
        }
    }
}
