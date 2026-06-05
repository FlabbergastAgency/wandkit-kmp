package com.flabbergast.wandkit.ui.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.root.WandKitComponent
import com.flabbergast.wandkit.ui.compose.feedbackForm.FeedbackFormView

@Composable
internal fun WandKitRootView(
    component: WandKitComponent,
) {
    val slotStack by component.slot.subscribeAsState()
    slotStack.child?.instance?.let { slot ->
        when (slot) {
            is WandKitComponent.Child.FeedbackForm -> FeedbackFormView(slot.component)
        }
    }
}