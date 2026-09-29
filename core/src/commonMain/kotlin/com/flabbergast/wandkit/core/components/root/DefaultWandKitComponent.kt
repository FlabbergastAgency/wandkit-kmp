package com.flabbergast.wandkit.core.components.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.featurepreview.FeaturePreviewComponentFactory
import com.flabbergast.wandkit.core.components.feedbackForm.FeedbackFormComponentFactory
import com.flabbergast.wandkit.core.components.screenshotPrompt.ScreenshotPromptComponentFactory
import com.flabbergast.wandkit.core.components.utils.componentScope
import com.flabbergast.wandkit.core.domain.featurepreview.FeaturePreviewController
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.domain.screenshot.ScreenshotPromptController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultWandKitComponent(
    componentContext: ComponentContext,
    formController: FeedbackFormController,
    screenshotPromptController: ScreenshotPromptController,
    featurePreviewController: FeaturePreviewController,
    accessGateBlocking: Flow<Boolean> = flowOf(false),
): WandKitComponent, ComponentContext by componentContext {
    private val navigation = SlotNavigation<Config>()

    override val slot: Value<ChildSlot<*, WandKitComponent.Child>> =
        childSlot(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = { null },
            childFactory = ::child,
        )

    init {
        // One slot, three publishers. A survey always wins: neither the
        // screenshot gate nor a feature-preview publish ever shows while a
        // form is up, and a form arriving while either is up simply covers
        // it. Between the other two, a feature preview - a deliberate,
        // host-triggered action - takes priority over an incidental
        // screenshot card. And while the invite gate is checking or blocked,
        // nothing shows at all: its Activity covers the app, and whatever was
        // up comes back once the gate lets the user through.
        componentScope.launch {
            combine(
                formController.form,
                featurePreviewController.prompt,
                screenshotPromptController.prompt,
                accessGateBlocking,
            ) { form, featurePreview, prompt, isGateBlocking ->
                when {
                    isGateBlocking -> null
                    form != null -> Config.FeedbackForm(form.entryPage.id)
                    featurePreview != null -> Config.FeaturePreview
                    prompt != null -> Config.ScreenshotPrompt
                    else -> null
                }
            }.distinctUntilChanged().collect { config ->
                if (config != null) {
                    navigation.activate(config)
                } else {
                    navigation.dismiss()
                }
            }
        }
    }

    override fun onBackClicked() {
        navigation.dismiss()
    }

    private fun child(
        config: Config,
        context: ComponentContext,
    ): WandKitComponent.Child = when (config) {
        is Config.FeedbackForm -> WandKitComponent.Child.FeedbackForm(
            FeedbackFormComponentFactory.get().create(context, config.entryPageId)
        )
        is Config.ScreenshotPrompt -> WandKitComponent.Child.ScreenshotPrompt(
            ScreenshotPromptComponentFactory.get().create(context)
        )
        is Config.FeaturePreview -> WandKitComponent.Child.FeaturePreview(
            FeaturePreviewComponentFactory.get().create(context)
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class FeedbackForm(val entryPageId: FeedbackFormPageId): Config

        /**
         * Carries nothing on purpose: this goes into the saved-state bundle,
         * and the screenshot bytes live in the controller instead.
         */
        @Serializable
        data object ScreenshotPrompt: Config

        /** Carries nothing on purpose: the resolved post state lives in the controller instead. */
        @Serializable
        data object FeaturePreview: Config
    }
}