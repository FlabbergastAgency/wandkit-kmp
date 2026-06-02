package com.flabbergast.wandkit.core.domain.forms

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal interface FeedbackFormController {
    val form: Flow<FeedbackForm?>

    fun publish(form: FeedbackForm)
    fun dismiss(formId: String?)
}

internal fun createFeedbackFormController(logger: Logger): FeedbackFormController = FeedbackFormControllerImpl(logger)

private const val LOGGER_TAG = "[FeedbackFormController]"

private class FeedbackFormControllerImpl(
    private val logger: Logger,
) : FeedbackFormController {
    private val _form = MutableStateFlow<FeedbackForm?>(null)
    override val form: Flow<FeedbackForm?> = _form

    override fun publish(form: FeedbackForm) {
        _form.update { current ->
            current ?: form.also {
                logger.debug(LOGGER_TAG, "Published form with formId: $form")
            }
        }
    }

    override fun dismiss(formId: String?) {
        _form.update { current ->
            if (current == null) return@update null

            if (formId == null || current.formId == formId) {
                logger.debug(LOGGER_TAG, "Dismissed form with formId: ${current.formId}")
                null
            } else {
                current
            }
        }
    }
}