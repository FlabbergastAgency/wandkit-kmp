package com.flabbergast.wandkit.core.components.screenshotPrompt

import com.arkivanov.decompose.ComponentContext
import com.flabbergast.wandkit.core.di.WandKitSdkContainer

internal fun interface ScreenshotPromptComponentFactory {
    fun create(context: ComponentContext): ScreenshotPromptComponent

    companion object {
        fun get(): ScreenshotPromptComponentFactory = Default(WandKitSdkContainer.get())
    }

    private class Default(
        private val sdkContainer: WandKitSdkContainer,
    ) : ScreenshotPromptComponentFactory {
        override fun create(context: ComponentContext) = DefaultScreenshotPromptComponent(
            componentContext = context,
            controller = sdkContainer.screenshotPromptController,
        )
    }
}
