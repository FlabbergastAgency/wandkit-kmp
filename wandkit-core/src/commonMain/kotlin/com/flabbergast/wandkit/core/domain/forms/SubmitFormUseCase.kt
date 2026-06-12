package com.flabbergast.wandkit.core.domain.forms

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.domain.forms.models.PageInput
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

internal interface SubmitFormUseCase {
    suspend operator fun invoke(results: Map<FeedbackFormPageId, PageInput>, dismissForm: Boolean)
}

internal fun createSubmitFormUseCase(
    formRepository: FeedbackFormRepository,
    formController: FeedbackFormController,
): SubmitFormUseCase = DefaultSubmitFormUseCase(
    formRepository = formRepository,
    formController = formController,
)

private class DefaultSubmitFormUseCase(
    private val formRepository: FeedbackFormRepository,
    private val formController: FeedbackFormController,
): SubmitFormUseCase {
    override suspend fun invoke(results: Map<FeedbackFormPageId, PageInput>, dismissForm: Boolean) {
        val impressionId = if (dismissForm) formController.dismiss() else getImpressionId()
        formController.submit()
        impressionId?.let {
            formRepository.submit(it, results)
        }
    }

    private suspend fun getImpressionId() = withTimeoutOrNull(500) {
        val isSubmitted = formController.isSubmitted.firstOrNull()
        if (isSubmitted == true) return@withTimeoutOrNull null
        formController.form.firstOrNull()?.impressionId
    }
}
