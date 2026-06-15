package com.flabbergast.wandkit.core.domain.forms

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

internal interface DismissFormUseCase {
    suspend operator fun invoke()
}

internal fun createDismissFormUseCase(
    formRepository: FeedbackFormRepository,
    formController: FeedbackFormController,
): DismissFormUseCase = DefaultDismissFormUseCase(
    formRepository = formRepository,
    formController = formController,
)

private class DefaultDismissFormUseCase(
    private val formRepository: FeedbackFormRepository,
    private val formController: FeedbackFormController,
): DismissFormUseCase {
    override suspend fun invoke() {
        val impressionId = formController.dismiss()
        impressionId?.let {
            formRepository.dismiss(it)
        }
    }
}
