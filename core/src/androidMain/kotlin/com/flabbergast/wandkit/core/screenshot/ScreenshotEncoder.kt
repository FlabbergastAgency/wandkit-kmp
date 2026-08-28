package com.flabbergast.wandkit.core.screenshot

import android.graphics.Bitmap
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Encodes a captured screenshot [Bitmap] into an attachment the composer can
 * upload.
 *
 * JPEG over PNG: a PNG of a photo-heavy screen (a full-bleed image in the
 * host app, a video frame) balloons in size, while JPEG at quality 80 keeps
 * UI text legible and keeps the base64 payload - which rides inside a
 * document-start script injected into the feedback webview - comfortably
 * under [DEFAULT_BYTE_CAP].
 */
internal object ScreenshotEncoder {
    /** Roughly 2 MB of base64 once encoded; comfortably clear of typical WebView script-injection limits. */
    internal const val DEFAULT_BYTE_CAP: Int = 1_500_000

    private val QUALITIES = intArrayOf(80, 60, 45)

    /**
     * Downscales [bitmap] so its long edge is at most [maxPixelSize], then
     * compresses to JPEG, trying [QUALITIES] in order and halving the size
     * once more if even the lowest quality does not fit [byteCap]. Returns
     * `null` when nothing tried fits - the caller drops the screenshot rather
     * than upload something oversized.
     *
     * Only recycles bitmaps this function allocated; [bitmap] itself is the
     * caller's and is left untouched.
     */
    internal fun jpegAttachment(
        bitmap: Bitmap,
        fileName: String,
        maxPixelSize: Int = 2000,
        byteCap: Int = DEFAULT_BYTE_CAP,
    ): WandKitComposerAttachment? {
        val created = mutableListOf<Bitmap>()
        try {
            var candidate = scaleTo(bitmap, maxPixelSize)
            if (candidate !== bitmap) created += candidate

            var bytes = compressBestEffort(candidate, byteCap)
            if (bytes == null) {
                val halved = scaleTo(candidate, max(1, longEdge(candidate) / 2))
                if (halved !== candidate) created += halved
                candidate = halved
                bytes = compressBestEffort(candidate, byteCap)
            }

            return bytes?.let { data ->
                WandKitComposerAttachment(data = data, contentType = "image/jpeg", fileName = fileName)
            }
        } finally {
            created.forEach { it.recycle() }
        }
    }

    private fun compressBestEffort(bitmap: Bitmap, byteCap: Int): ByteArray? {
        for (quality in QUALITIES) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val bytes = stream.toByteArray()
            if (bytes.size <= byteCap) return bytes
        }
        return null
    }

    private fun longEdge(bitmap: Bitmap): Int = max(bitmap.width, bitmap.height)

    private fun scaleTo(bitmap: Bitmap, maxPixelSize: Int): Bitmap {
        val edge = longEdge(bitmap)
        if (edge <= maxPixelSize) return bitmap

        val scale = maxPixelSize.toDouble() / edge
        val width = max(1, (bitmap.width * scale).toInt())
        val height = max(1, (bitmap.height * scale).toInt())
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}

/**
 * Encodes an arbitrary [Bitmap] as a composer attachment - the Android
 * counterpart to picking an image from disk, for hosts that want to seed
 * [com.flabbergast.wandkit.core.feedback.WandKitComposerPrefill] with an
 * image of their own choosing rather than a captured screenshot.
 */
public fun WandKitComposerAttachment.Companion.image(
    bitmap: Bitmap,
    fileName: String = "image.jpg",
    maxPixelSize: Int = 2000,
): WandKitComposerAttachment? = ScreenshotEncoder.jpegAttachment(bitmap, fileName, maxPixelSize)
