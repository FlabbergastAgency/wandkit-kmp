package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.feedbackForm.FeedbackFormComponent
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalCard
import com.flabbergast.wandkit.ui.compose.shared.WandKitModalScrim

@Composable
internal fun FeedbackFormView(
    component: FeedbackFormComponent,
    contentAlignment: Alignment,
) {
    val stack by component.stack.subscribeAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = contentAlignment,
    ) {
        WandKitModalScrim(onDismiss = component::dismissForm)

        WandKitModalCard {
            AnimatedContent(
                targetState = stack.active.instance,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 300)) using
                        SizeTransform(clip = false)
                },
                label = "feedback-form-page",
            ) { activeChild ->
                when (activeChild) {
                    is FeedbackFormComponent.Child.FormPage -> FormPageView(activeChild.component)
                }
            }
        }
    }
}
