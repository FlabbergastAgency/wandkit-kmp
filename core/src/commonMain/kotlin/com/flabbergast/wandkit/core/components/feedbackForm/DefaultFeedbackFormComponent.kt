package com.flabbergast.wandkit.core.components.feedbackForm

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.flabbergast.wandkit.core.components.formPage.FormPageComponentFactory
import com.flabbergast.wandkit.core.components.utils.componentScope
import com.flabbergast.wandkit.core.domain.forms.DismissFormUseCase
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.SubmitFormUseCase
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.FireAndForgetTask
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.forms.models.PageInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

private const val LOGGER_TAG = "[FeedbackFormComponent]"
private const val FETCH_TIMEOUT_MILLIS = 500L

internal class DefaultFeedbackFormComponent(
    componentContext: ComponentContext,
    entryPageId: FeedbackFormPageId,
    private val formController: FeedbackFormController,
    private val dismissFormUseCase: DismissFormUseCase,
    private val submitFormUseCase: SubmitFormUseCase,
    private val fireAndForgetTask: FireAndForgetTask,
    private val logger: Logger,
): FeedbackFormComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    private val pageResults = MutableStateFlow<Map<FeedbackFormPageId, PageInput>>(mapOf())

    override val stack: Value<ChildStack<*, FeedbackFormComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.FormPage(entryPageId),
            childFactory = ::child,
        )

    init {
        componentScope.launch {
            formController.form.filterNotNull().collect { form ->
                navigation.replaceAll(Config.FormPage(form.entryPage.id))
            }
        }
    }

    override fun dismissForm() {
        fireAndForgetTask {
            dismissFormUseCase()
        }
    }

    private fun submitForm(dismissForm: Boolean = true) {
        fireAndForgetTask {
            submitFormUseCase(pageResults.value, dismissForm)
        }
    }

    private fun goToNextPage(pageId: FeedbackFormPageId, result: PageInput?) {
        componentScope.launch {
            val form = withTimeoutOrNull(FETCH_TIMEOUT_MILLIS) {
                formController.form.firstOrNull()
            }
            val currentPage = form?.pages[pageId] ?: run {
                logger.debug(LOGGER_TAG, "Couldn't find next page with id $pageId, submitting form.")
                submitForm()
                return@launch
            }
            val nextPageId = resolveNextPage(currentPage, result) ?: run {
                submitForm()
                return@launch
            }

            if (form.pages[nextPageId]?.content is FeedbackFormPage.Content.End) {
                submitForm(dismissForm = false)
            }

            navigation.push(Config.FormPage(nextPageId))
        }
    }

    private fun submitPage(pageId: FeedbackFormPageId, result: PageInput) {
        pageResults.update {
            it + (pageId to result)
        }
        goToNextPage(pageId, result)
    }

    private fun child(
        config: Config,
        context: ComponentContext,
    ) = when (config) {
        is Config.FormPage -> FeedbackFormComponent.Child.FormPage(
            FormPageComponentFactory.get().create(
                context = context,
                pageId = config.pageId,
                onDismissForm = ::dismissForm,
                onSubmitPage = ::submitPage,
                onSkipPage = { goToNextPage(it, null) }
            )
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class FormPage(val pageId: FeedbackFormPageId): Config
    }

    private fun resolveNextPage(
        current: FeedbackFormPage,
        result: PageInput?,
    ): FeedbackFormPageId? =
        current.next.firstNotNullOfOrNull { rule ->
            when (rule) {
                is FeedbackFormPage.NextPageRule.None -> rule.nextPageId
                is FeedbackFormPage.NextPageRule.Option -> rule.takeIf { result?.optionIds?.contains(it.optionId) ?: false }?.nextPageId
                is FeedbackFormPage.NextPageRule.Stars -> rule.takeIf { result?.stars == it.starRating }?.nextPageId
                is FeedbackFormPage.NextPageRule.Thumbs -> rule.takeIf { result?.isThumbsUp == it.isThumbsUp }?.nextPageId
            }
        }
}
