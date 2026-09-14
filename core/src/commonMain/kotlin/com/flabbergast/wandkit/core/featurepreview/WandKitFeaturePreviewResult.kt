package com.flabbergast.wandkit.core.featurepreview

/** How the feature-preview sheet was left. */
public enum class WandKitFeaturePreviewResult {
    /**
     * The coming-soon primary succeeded: the post was voted (and therefore
     * followed), and the feedback UI opened on it with a one-time
     * confirmation.
     */
    Subscribed,

    /** The secondary action was tapped: the host continues its own flow (e.g. "Continue with EASA"). */
    Secondary,

    /**
     * A backdrop tap, the back gesture, or Close on the generic fallback
     * sheet (shown when no preview is configured for the post, it is
     * disabled, unpublished, or the fetch failed).
     */
    Dismissed,

    /** The available-state primary: the store listing was opened. */
    UpdateApp,
}
