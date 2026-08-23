package com.flabbergast.wandkit.core.components.screenshotPrompt

import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment

/** The native screenshot report flow: card -> composer -> thank-you. */
public interface ScreenshotPromptComponent {
    public val viewState: Value<ViewState>

    /** Moves from the card to the text composer. */
    public fun onReport()

    public fun onTextChanged(text: String)

    /** Uploads the screenshot and creates the report post. */
    public fun onSend()

    public fun onDismiss()

    public data class ViewState(
        /**
         * The screenshot to preview. `null` only when the slot was restored
         * after process death before anything was captured; the view renders
         * nothing and the root dismisses the slot a moment later.
         */
        val attachment: WandKitComposerAttachment?,
        val phase: Phase,
    ) {
        public sealed interface Phase {
            public data object Prompt : Phase

            public data class Composing(
                val text: String,
                val isSending: Boolean,
                val error: String?,
            ) : Phase

            public data object Sent : Phase
        }
    }
}
