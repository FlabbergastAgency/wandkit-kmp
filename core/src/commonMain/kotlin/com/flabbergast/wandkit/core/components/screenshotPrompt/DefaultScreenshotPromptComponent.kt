package com.flabbergast.wandkit.core.components.screenshotPrompt

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.utils.componentScope
import com.flabbergast.wandkit.core.components.utils.toValue
import com.flabbergast.wandkit.core.domain.screenshot.ScreenshotPrompt
import com.flabbergast.wandkit.core.domain.screenshot.ScreenshotPromptController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class DefaultScreenshotPromptComponent(
    componentContext: ComponentContext,
    private val controller: ScreenshotPromptController,
) : ScreenshotPromptComponent, ComponentContext by componentContext {

    override val viewState: Value<ScreenshotPromptComponent.ViewState> =
        controller.prompt
            .map { it.toViewState() }
            .stateIn(
                scope = componentScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = controller.prompt.value.toViewState(),
            )
            .toValue(componentScope)

    override fun onReport() = controller.report()

    override fun onTextChanged(text: String) = controller.updateText(text)

    override fun onSend() = controller.send()

    override fun onDismiss() = controller.dismiss()
}

private fun ScreenshotPrompt?.toViewState(): ScreenshotPromptComponent.ViewState =
    ScreenshotPromptComponent.ViewState(
        attachment = this?.attachment,
        phase = when (val phase = this?.phase) {
            null, is ScreenshotPrompt.Phase.Prompt -> ScreenshotPromptComponent.ViewState.Phase.Prompt
            is ScreenshotPrompt.Phase.Composing -> ScreenshotPromptComponent.ViewState.Phase.Composing(
                text = phase.text,
                isSending = phase.isSending,
                error = phase.error,
            )
            is ScreenshotPrompt.Phase.Sent -> ScreenshotPromptComponent.ViewState.Phase.Sent
        },
    )
