package com.flabbergast.wandkit.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.root.WandKitComponent
import com.flabbergast.wandkit.ui.compose.feedbackForm.FeedbackFormView

@Composable
internal fun WandKitRootView(
    component: WandKitComponent,
) {
    val slotStack by component.slot.subscribeAsState()

    AnimatedContent(
        targetState = slotStack.child?.instance,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(
                    fadeOut(animationSpec = tween(220)) +
                            scaleOut(
                                targetScale = 0.92f,
                                animationSpec = tween(220, delayMillis = 90)
                            )
                ) using
                    SizeTransform(clip = false)
        },
        label = "wandkit-root-slot",
    ) { slot ->
        when (slot) {
            is WandKitComponent.Child.FeedbackForm -> FeedbackFormView(slot.component)
            null -> Unit
        }
    }
}
