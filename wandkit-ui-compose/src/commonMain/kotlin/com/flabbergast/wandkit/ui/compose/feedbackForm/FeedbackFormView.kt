package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.flabbergast.wandkit.core.components.feedbackForm.FeedbackFormComponent

@Composable
internal fun FeedbackFormView(
    component: FeedbackFormComponent,
) {
    Children(component.stack) { child ->
        when (val instance = child.instance) {
            is FeedbackFormComponent.Child.FormPage -> FormPageView(instance.component)
        }

    }
}