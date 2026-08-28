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

    /**
     * The user-confirmed name from a `display_name` page, if one was shown
     * and answered. Kept separate from [pageResults] because it is never an
     * `answers[]` entry - it travels as its own top-level field on submit.
     */
    private val displayName = MutableStateFlow<String?>(null)

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
                val visibleEntryPageId = resolveVisiblePageId(form, pageResults.value, form.entryPage.id)
                    ?: form.entryPage.id
                navigation.replaceAll(Config.FormPage(visibleEntryPageId))
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
            submitFormUseCase(pageResults.value, dismissForm, displayName.value)
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
            val resolvedPageId = resolveNextPageId(currentPage, result) ?: run {
                submitForm()
                return@launch
            }
            val nextPageId = resolveVisiblePageId(form, pageResults.value, resolvedPageId) ?: run {
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
        val page = formController.form.value?.pages?.get(pageId)
        if (page?.content is FeedbackFormPage.Content.DisplayName) {
            // Never an `answers[]` entry - travels as its own submit field.
            displayName.update { current ->
                resolveDisplayNameAfterAdvance(current, page.content, PageAdvanceAction.CONTINUE, result.text)
            }
        } else {
            pageResults.update {
                it + (pageId to result)
            }
        }
        goToNextPage(pageId, result)
    }

    /**
     * Explicit skip via the secondary button. Never routes through
     * [submitPage]: a `display_name` page exists to get consent for the
     * name, so a skip must clear any pending confirmation rather than
     * silently committing a prefilled suggestion the user never confirmed.
     */
    private fun skipPage(pageId: FeedbackFormPageId) {
        formController.form.value?.pages?.get(pageId)?.content?.let { content ->
            displayName.update { current ->
                resolveDisplayNameAfterAdvance(current, content, PageAdvanceAction.SKIP, rawText = null)
            }
        }
        goToNextPage(pageId, null)
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
                onSkipPage = ::skipPage,
            )
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class FormPage(val pageId: FeedbackFormPageId): Config
    }
}
