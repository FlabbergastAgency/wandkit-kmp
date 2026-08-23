package com.flabbergast.wandkit.core.feedback

/**
 * The post types the feedback board knows. [wireValue] is what the web
 * composer's type picker uses.
 */
public enum class WandKitPostType(public val wireValue: String) {
    BUG("bug"),
    FEATURE_REQUEST("feature_request"),
    IMPROVEMENT("improvement"),
    QUESTION("question"),
    PRAISE("praise"),
    OTHER("other"),
}

/**
 * A file the composer opens with already attached and uploading, as if the
 * user had just picked it.
 *
 * Holds encoded bytes rather than a platform image: the bytes are what the
 * composer needs, and they cross the process-internal hand-off to the feedback
 * screen unchanged. On Android, `WandKitComposerAttachment.image(bitmap)` does
 * the encoding for you.
 *
 * A plain class rather than a data class on purpose: a `ByteArray` property
 * would make the generated `equals` compare references.
 */
public class WandKitComposerAttachment(
    public val data: ByteArray,
    /**
     * One of `image/png`, `image/jpeg`, `image/heic`, `image/webp` - the
     * formats the backend accepts. Anything else is dropped by the composer.
     */
    public val contentType: String,
    public val fileName: String,
) {
    public val kind: String = "image"

    override fun equals(other: Any?): Boolean =
        other is WandKitComposerAttachment &&
            other.contentType == contentType &&
            other.fileName == fileName &&
            other.data.contentEquals(data)

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }

    override fun toString(): String =
        "WandKitComposerAttachment(kind=$kind, contentType=$contentType, fileName=$fileName, bytes=${data.size})"

    public companion object
}

/**
 * Seed content for the new-post composer.
 *
 * Everything is optional; an empty prefill opens a blank composer. The web
 * app consumes it once, and ignores a [type] the project has disabled.
 */
public class WandKitComposerPrefill(
    @Deprecated("The composer has a single text field; use description")
    public val title: String? = null,
    public val description: String? = null,
    /** Pre-selects the type picker. */
    public val type: WandKitPostType? = null,
    /** Up to 5 images; the composer enforces its own limits on top. */
    public val attachments: List<WandKitComposerAttachment> = emptyList(),
) {
    /**
     * True when nothing would be seeded, in which case the bootstrap omits
     * `prefill` entirely.
     */
    internal val isEmpty: Boolean
        get() = title.isNullOrBlank() && description.isNullOrBlank() && type == null && attachments.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is WandKitComposerPrefill &&
            other.title == title &&
            other.description == description &&
            other.type == type &&
            other.attachments == attachments

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + attachments.hashCode()
        return result
    }

    override fun toString(): String =
        "WandKitComposerPrefill(title=$title, description=$description, type=$type, attachments=$attachments)"
}
