package com.flabbergast.wandkit.core.feedback

/** Which screen of the feedback web app to open on. */
public sealed interface WandKitFeedbackScreen {
    /** The feed - the default entry point. */
    public data object Feed : WandKitFeedbackScreen

    /**
     * The new-post composer, optionally seeded (title, description, type,
     * attachments). Read-only (anonymous) sessions cannot compose; the web app
     * degrades them to the feed.
     */
    public data class Composer(
        val prefill: WandKitComposerPrefill = WandKitComposerPrefill(),
    ) : WandKitFeedbackScreen

    /**
     * A single post's detail screen. Read-only (anonymous) sessions can view
     * it the same as any other post.
     */
    public data class Post(
        val publicId: String,
    ) : WandKitFeedbackScreen
}
