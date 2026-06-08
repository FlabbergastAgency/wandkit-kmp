package com.flabbergast.wandkit.core.components.formPage

import com.arkivanov.decompose.ComponentContext
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.components.formPage.model.PageInput

internal fun interface FormPageComponentFactory {
    fun create(
        context: ComponentContext,
        pageId: FeedbackFormPageId,
        onDismissForm: () -> Unit,
        onSubmitPage: (pageId: FeedbackFormPageId, result: PageInput) -> Unit,
        onSkipPage: (pageId: FeedbackFormPageId) -> Unit,
    ): FormPageComponent

    companion object {
        fun get(): FormPageComponentFactory = Default(WandKitSdkContainer.get())
    }

    private class Default(
        private val sdkContainer: WandKitSdkContainer,
    ) : FormPageComponentFactory {
        override fun create(
            context: ComponentContext,
            pageId: FeedbackFormPageId,
            onDismissForm: () -> Unit,
            onSubmitPage: (pageId: FeedbackFormPageId, result: PageInput) -> Unit,
            onSkipPage: (pageId: FeedbackFormPageId) -> Unit,
        ) = DefaultFormPageComponent(
            componentContext = context,
            formController = sdkContainer.feedbackFormController,
            pageId = pageId,
            onDismissForm = onDismissForm,
            onSubmitPage = onSubmitPage,
            onSkipPage = onSkipPage,
        )
    }
}