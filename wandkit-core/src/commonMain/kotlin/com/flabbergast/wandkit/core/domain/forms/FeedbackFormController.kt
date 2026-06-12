package com.flabbergast.wandkit.core.domain.forms

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.forms.models.ImpressionId
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal interface FeedbackFormController {
    val form: Flow<FeedbackForm?>
    val isSubmitted: Flow<Boolean?>

    fun publish(form: FeedbackForm)
    fun dismiss(): ImpressionId?

    fun submit()
}

internal fun createFeedbackFormController(logger: Logger): FeedbackFormController = FeedbackFormControllerImpl(logger)

private const val LOGGER_TAG = "[FeedbackFormController]"

private class FeedbackFormControllerImpl(
    private val logger: Logger,
) : FeedbackFormController {
    private val _form = MutableStateFlow<FeedbackForm?>(null)
    override val form: Flow<FeedbackForm?> = _form

    private val _isSubmitted = MutableStateFlow<Boolean?>(null)
    override val isSubmitted: Flow<Boolean?> = _isSubmitted

    override fun publish(form: FeedbackForm) {
        _form.update { current ->
            current ?: form.also {
                _isSubmitted.value = false
                logger.debug(LOGGER_TAG, "Published form with formId: ${form.formId}")
            }
        }
    }

    override fun dismiss(): ImpressionId? {
        val impressionId = _form.value?.impressionId
        val isSubmitted = _isSubmitted.value == true
        _form.update { current ->
            if (current == null) return@update null
            _isSubmitted.value = null
            logger.debug(LOGGER_TAG, "Dismissed form with formId: ${current.formId}")
            null
        }
        return impressionId?.takeIf { !isSubmitted }
    }

    override fun submit() {
        _isSubmitted.update { old -> if (old != null) true else old }
    }
}