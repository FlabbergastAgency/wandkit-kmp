package com.flabbergast.wandkit.core.components.featurepreview

import com.arkivanov.decompose.ComponentContext
import com.flabbergast.wandkit.core.di.WandKitSdkContainer

internal fun interface FeaturePreviewComponentFactory {
    fun create(context: ComponentContext): FeaturePreviewComponent

    companion object {
        fun get(): FeaturePreviewComponentFactory = Default(WandKitSdkContainer.get())
    }

    private class Default(
        private val sdkContainer: WandKitSdkContainer,
    ) : FeaturePreviewComponentFactory {
        override fun create(context: ComponentContext) = DefaultFeaturePreviewComponent(
            componentContext = context,
            controller = sdkContainer.featurePreviewController,
        )
    }
}
