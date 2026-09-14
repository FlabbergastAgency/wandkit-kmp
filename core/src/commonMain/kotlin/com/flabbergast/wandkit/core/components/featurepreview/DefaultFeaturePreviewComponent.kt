package com.flabbergast.wandkit.core.components.featurepreview

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.utils.componentScope
import com.flabbergast.wandkit.core.components.utils.toValue
import com.flabbergast.wandkit.core.domain.featurepreview.FeaturePreviewController
import com.flabbergast.wandkit.core.domain.featurepreview.FeaturePreviewPrompt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class DefaultFeaturePreviewComponent(
    componentContext: ComponentContext,
    private val controller: FeaturePreviewController,
) : FeaturePreviewComponent, ComponentContext by componentContext {

    override val viewState: Value<FeaturePreviewComponent.ViewState> =
        controller.prompt
            .map { it.toViewState() }
            .stateIn(
                scope = componentScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = controller.prompt.value.toViewState(),
            )
            .toValue(componentScope)

    override fun onPrimary() {
        when (controller.prompt.value?.state) {
            is FeaturePreviewPrompt.State.ComingSoon -> controller.onPrimary()
            is FeaturePreviewPrompt.State.Available -> controller.onUpdateApp()
            FeaturePreviewPrompt.State.Generic -> controller.onDismiss()
            null -> Unit
        }
    }

    override fun onSecondary() = controller.onSecondary()

    override fun onDismiss() = controller.onDismiss()
}

private fun FeaturePreviewPrompt?.toViewState(): FeaturePreviewComponent.ViewState =
    FeaturePreviewComponent.ViewState(content = this?.toContent())

private fun FeaturePreviewPrompt.toContent(): FeaturePreviewComponent.ViewState.Content = when (val current = state) {
    is FeaturePreviewPrompt.State.ComingSoon -> FeaturePreviewComponent.ViewState.Content.ComingSoon(
        title = previewCopy.title,
        message = previewCopy.message,
        secondaryLabel = previewCopy.secondaryLabel,
        primaryLabel = previewCopy.primaryLabel,
        isPrimaryEnabled = !current.isVoting,
        isVoting = current.isVoting,
        error = current.error,
    )

    is FeaturePreviewPrompt.State.Available -> FeaturePreviewComponent.ViewState.Content.Available(
        title = previewCopy.title,
        message = previewCopy.message,
        secondaryLabel = previewCopy.secondaryLabel,
        primaryLabel = previewCopy.primaryLabel,
        storeUrl = current.storeUrl,
    )

    FeaturePreviewPrompt.State.Generic -> FeaturePreviewComponent.ViewState.Content.Generic(
        title = previewCopy.title,
        message = previewCopy.message,
        primaryLabel = previewCopy.primaryLabel,
    )
}
