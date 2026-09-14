package com.flabbergast.wandkit.core.components.featurepreview

import com.arkivanov.decompose.value.Value

/** The native "coming soon / it's here" sheet: see `WandKit.presentFeaturePreview`. */
public interface FeaturePreviewComponent {
    public val viewState: Value<ViewState>

    /**
     * Coming-soon: votes (auto-follows) unless already voted, then opens the
     * feedback screen on the post. Available: the view has already opened
     * the store link - this just concludes the flow. Generic: dismisses the
     * sheet (Close). No-op once the sheet has nothing to show.
     */
    public fun onPrimary()

    /** Secondary tap: the host continues its own flow. */
    public fun onSecondary()

    /** Backdrop tap, or back press. */
    public fun onDismiss()

    public data class ViewState(
        /**
         * `null` only in the brief window before the root dismisses this
         * slot - mirrors [com.flabbergast.wandkit.core.components.screenshotPrompt.ScreenshotPromptComponent].
         */
        val content: Content?,
    ) {
        public sealed interface Content {
            public val title: String
            public val message: String

            public data class ComingSoon(
                override val title: String,
                override val message: String,
                val secondaryLabel: String,
                val primaryLabel: String,
                val isPrimaryEnabled: Boolean,
                val isVoting: Boolean,
                val error: String?,
            ) : Content

            public data class Available(
                override val title: String,
                override val message: String,
                val secondaryLabel: String,
                val primaryLabel: String,
                /** `null` hides the primary button - no store URL configured for this project. */
                val storeUrl: String?,
            ) : Content

            /** Generic "coming soon" fallback: a single primary action (Close), no secondary. */
            public data class Generic(
                override val title: String,
                override val message: String,
                val primaryLabel: String,
            ) : Content
        }
    }
}
