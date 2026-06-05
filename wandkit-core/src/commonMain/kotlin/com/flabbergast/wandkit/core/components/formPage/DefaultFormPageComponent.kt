package com.flabbergast.wandkit.core.components.formPage

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.formPage.mapper.formPageViewStateMapper
import com.flabbergast.wandkit.core.components.utils.componentScope
import com.flabbergast.wandkit.core.components.utils.toValue
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.components.formPage.model.PageInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class DefaultFormPageComponent(
    private val pageId: FeedbackFormPageId,
    formController: FeedbackFormController,
    private val onDismissForm: () -> Unit,
    private val onSubmitPage: (pageId: FeedbackFormPageId, result: PageInput) -> Unit,
    componentContext: ComponentContext,
): FormPageComponent, ComponentContext by componentContext {

    private val input = MutableStateFlow(PageInput())

    private val page = formController.form
        .map { form -> form?.pages[pageId] }

    override val viewState: Value<FormPageComponent.ViewState> = combine(input, page) { input, page ->
        FormPageComponent.ViewState(formPageViewStateMapper(input, page))
    }.stateIn(
        scope = componentScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FormPageComponent.ViewState(null),
    ).toValue(componentScope)

    override fun dismissForm() = onDismissForm()

    override fun submitPage() {
        onSubmitPage(pageId, input.value)
    }

    override fun updateThumbs(isThumbsUp: Boolean) {
        input.update {
            it.copy(isThumbsUp = isThumbsUp)
        }
    }

    override fun updateStars(stars: Int) {
        input.update {
            it.copy(stars = stars)
        }
    }

    override fun updateText(text: String) {
        input.update {
            it.copy(text = text)
        }
    }

    override fun updateMultiChoice(optionId: String) {
        input.update { current ->
            current.copy(optionIds = current.optionIds?.let { it + optionId } ?: listOf(optionId))
        }
    }
}