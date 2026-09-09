package com.flabbergast.wandkit.core.feedback

/**
 * A file the host app attaches to a screenshot report for the team's eyes
 * only - shown in the dashboard, never to the end user.
 *
 * Holds encoded bytes rather than a platform file handle: the bytes are what
 * the report needs, and they cross the process-internal hand-off to the
 * upload pipeline unchanged.
 *
 * A plain class rather than a data class on purpose: a `ByteArray` property
 * would make the generated `equals` compare references, same rationale as
 * [WandKitComposerPrefill]'s attachment type.
 */
public class WandKitDebugAttachment(
    public val data: ByteArray,
    public val fileName: String,
    public val contentType: String = "application/octet-stream",
) {
    override fun equals(other: Any?): Boolean =
        other is WandKitDebugAttachment &&
            other.fileName == fileName &&
            other.contentType == contentType &&
            other.data.contentEquals(data)

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }

    override fun toString(): String =
        "WandKitDebugAttachment(fileName=$fileName, contentType=$contentType, bytes=${data.size})"

    public companion object {
        /** UTF-8 text as `text/plain; charset=utf-8`. */
        public fun text(text: String, fileName: String): WandKitDebugAttachment = WandKitDebugAttachment(
            data = text.encodeToByteArray(),
            fileName = fileName,
            contentType = "text/plain; charset=utf-8",
        )
    }
}

/**
 * Supplies debug attachments (logs, JSON dumps, ...) for a screenshot report.
 *
 * Called on the report card's Send, before any upload starts. Return the
 * files to attach; throw or return an empty list to attach nothing - either
 * way the report still goes out with the screenshot alone.
 */
public fun interface WandKitDebugAttachmentsProvider {
    public suspend fun provide(): List<WandKitDebugAttachment>
}
