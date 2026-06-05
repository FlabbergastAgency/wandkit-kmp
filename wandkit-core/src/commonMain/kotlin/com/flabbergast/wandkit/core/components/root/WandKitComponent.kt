package com.flabbergast.wandkit.core.components.root

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.feedbackForm.FeedbackFormComponent

public interface WandKitComponent {
    public val slot: Value<ChildSlot<*, Child>>

    public fun onBackClicked()

    public sealed interface Child {
        public data class FeedbackForm(
            val component: FeedbackFormComponent,
        ): Child
    }
}