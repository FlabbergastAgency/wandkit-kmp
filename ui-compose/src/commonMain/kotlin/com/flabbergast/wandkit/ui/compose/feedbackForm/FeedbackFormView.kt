package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.feedbackForm.FeedbackFormComponent
import com.flabbergast.wandkit.ui.compose.WandKitColors

@Composable
internal fun FeedbackFormView(
    component: FeedbackFormComponent,
    contentAlignment: Alignment,
) {
    val stack by component.stack.subscribeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(component) {
                    detectTapGestures(onTap = {
                        component.dismissForm()
                    })
                }
        )

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(1.dp, WandKitColors.quaternaryLabel, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .widthIn(max = 560.dp),
            color = WandKitColors.systemBackground,
            contentColor = WandKitColors.label,
        ) {
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
