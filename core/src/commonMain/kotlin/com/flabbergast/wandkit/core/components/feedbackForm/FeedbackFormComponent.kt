package com.flabbergast.wandkit.core.components.feedbackForm

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.formPage.FormPageComponent

public interface FeedbackFormComponent {
    public val stack: Value<ChildStack<*, Child>>

    public fun dismissForm()

    public interface Child {
        public data class FormPage(val component: FormPageComponent): Child
    }
}
