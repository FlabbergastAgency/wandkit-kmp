package com.flabbergast.wandkit.core.components.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.feedbackForm.FeedbackFormComponentFactory
import com.flabbergast.wandkit.core.components.utils.componentScope
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultWandKitComponent(
    componentContext: ComponentContext,
    formController: FeedbackFormController,
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
        componentScope.launch {
            formController.form.collect { form ->
                if (form != null) {
                    navigation.activate(Config.FeedbackForm(form.entryPage.id))
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
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class FeedbackForm(val entryPageId: FeedbackFormPageId): Config
    }
}