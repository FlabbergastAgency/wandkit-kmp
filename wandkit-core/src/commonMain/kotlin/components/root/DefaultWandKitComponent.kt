package com.flabbergast.wandkit.core.components.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import kotlinx.serialization.Serializable

internal class DefaultWandKitComponent(
    componentContext: ComponentContext,
    private val eventsRepository: EventsRepository,
): WandKitComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, WandKitComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Test,
            childFactory = ::child,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun child(
        config: Config,
        childComponentContext: ComponentContext,
    ): WandKitComponent.Child = WandKitComponent.Child.Test

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Test: Config
    }
}