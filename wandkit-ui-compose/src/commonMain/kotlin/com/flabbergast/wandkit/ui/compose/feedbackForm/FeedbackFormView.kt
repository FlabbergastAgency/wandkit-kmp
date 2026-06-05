package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.feedbackForm.FeedbackFormComponent

@Composable
internal fun FeedbackFormView(
    component: FeedbackFormComponent,
) {
    val stack by component.stack.subscribeAsState()

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 560.dp),
        ) {
            AnimatedContent(
                targetState = stack.active.instance,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 180)) using
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
