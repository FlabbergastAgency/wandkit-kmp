package com.flabbergast.wandkit.core.domain.forms

internal interface DismissFormUseCase {
    suspend operator fun invoke()
}

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