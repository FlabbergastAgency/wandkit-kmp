package com.flabbergast.wandkit.core.components.root

import com.arkivanov.decompose.ComponentContext
import com.flabbergast.wandkit.core.components.ComponentFactory
import com.flabbergast.wandkit.core.di.WandKitSdkContainer

public interface WandKitComponentFactory {
    public fun create(context: ComponentContext): WandKitComponent

    public companion object {
        public fun get(): WandKitComponentFactory = WandKitComponentFactoryImpl(WandKitSdkContainer.also { println("[matko] getting container") }.get())
    }
}

internal class WandKitComponentFactoryImpl(
    override val container: WandKitSdkContainer,
): WandKitComponentFactory, ComponentFactory {
    override fun create(context: ComponentContext) = DefaultWandKitComponent(
        componentContext = context,
        eventsRepository = container.eventsRepository,
    )
}


