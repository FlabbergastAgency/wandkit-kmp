package com.flabbergast.wandkit.core.components.feedbackForm

import com.arkivanov.decompose.ComponentContext
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId

internal fun interface FeedbackFormComponentFactory {
    fun create(context: ComponentContext, entryPageId: FeedbackFormPageId): FeedbackFormComponent

    companion object {
        fun get(): FeedbackFormComponentFactory = Default(WandKitSdkContainer.get())
    }

    private class Default(
        private val sdkContainer: WandKitSdkContainer,
    ): FeedbackFormComponentFactory {
        override fun create(context: ComponentContext, entryPageId: FeedbackFormPageId) =
            DefaultFeedbackFormComponent(
                componentContext = context,
                entryPageId = entryPageId,
                formController = sdkContainer.feedbackFormController,
                logger = sdkContainer.logger,
            )
    }
}