package com.flabbergast.wandkit.core.components.formPage

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.formPage.mapper.formPageViewStateMapper
import com.flabbergast.wandkit.core.components.formPage.model.FormPageButton
import com.flabbergast.wandkit.core.components.utils.componentScope
import com.flabbergast.wandkit.core.components.utils.toValue
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.domain.forms.models.PageInput
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val DATA_TIMEOUT_MILLIS = 500L

internal class DefaultFormPageComponent(
    private val pageId: FeedbackFormPageId,
    formController: FeedbackFormController,
    private val onDismissForm: () -> Unit,
    private val onSubmitPage: (pageId: FeedbackFormPageId, result: PageInput) -> Unit,
    private val onSkipPage: (pageId: FeedbackFormPageId) -> Unit,
    componentContext: ComponentContext,
) : FormPageComponent, ComponentContext by componentContext {

    private val input = MutableStateFlow(PageInput())

    private val page = formController.form
        .map { form -> form?.pages[pageId] }

    private val shouldAutoContinueOnInput = page.map { it?.nextButtonLabel == null }

    override val viewState: Value<FormPageComponent.ViewState> =
        combine(input, page) { input, page ->
            FormPageComponent.ViewState(formPageViewStateMapper(input, page))
        }.stateIn(
            scope = componentScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormPageComponent.ViewState(null),
        ).toValue(componentScope)

    override fun dismissForm() = onDismissForm()

    override fun buttonAction(action: FormPageButton.Action) {
        when (action) {
            FormPageButton.Action.CONTINUE -> onSubmitPage(pageId, input.value)
            FormPageButton.Action.SKIP -> onSkipPage(pageId)
        }
    }

    override fun updateThumbs(isThumbsUp: Boolean) {
        input.update {
            it.copy(isThumbsUp = isThumbsUp)
        }
        autoContinueIfNeeded()
    }

    override fun updateStars(stars: Int) {
        input.update {
            it.copy(stars = stars)
        }
        autoContinueIfNeeded()
    }

    override fun updateText(text: String) {
        input.update {
            it.copy(text = text)
        }
    }

    override fun updateMultiChoice(optionId: String) {
        componentScope.launch {
            val pageContent = page.firstWithTimeout()?.content as? FeedbackFormPage.Content.MultiChoice
            val canSelectMultiple = pageContent?.allowMultiple ?: false

            input.update { current ->
                when {
                    current.optionIds?.contains(optionId) == true ->
                        current.copy(optionIds = current.optionIds - optionId)

                    canSelectMultiple ->
                        current.copy(
                            optionIds = current.optionIds?.let { it + optionId } ?: listOf(optionId)
                        )

                    else ->
                        current.copy(optionIds = listOf(optionId))
                }
            }

            if (!canSelectMultiple) {
                autoContinueIfNeeded()
            }
        }
    }

    private fun autoContinueIfNeeded() {
        componentScope.launch {
            if (shouldAutoContinueOnInput.firstWithTimeout() == true) {
                onSubmitPage(pageId, input.value)
            }
        }
    }

    private suspend fun <T> Flow<T>.firstWithTimeout() = withTimeoutOrNull(DATA_TIMEOUT_MILLIS) { firstOrNull() }
}