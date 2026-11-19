package com.flabbergast.wandkit.core.domain.forms

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.forms.models.ImpressionId
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

internal interface FeedbackFormController {
    /** A state flow so the screenshot gate can read "is a form up" synchronously. */
    val form: StateFlow<FeedbackForm?>
    val isSubmitted: StateFlow<Boolean?>

    fun publish(form: FeedbackForm)
    fun dismiss(): ImpressionId?

    fun submit()

    /**
     * A top rating on a `stars` page that requests the native review. Like
     * iOS, nothing takes it back - not a later rating change, nor skipping
     * the page; the form asks once it closes.
     */
    fun markNativeReviewRequested()
}

/**
 * Where the controller hands the native review off to the platform.
 */
internal interface NativeReviewHooks {
    /** A review was first marked for the current form: get ready to show it. */
    fun prefetch()

    /** The form closed with a review marked, completed or closed early - never mid-flow. */
    fun request()

    /** The form closed without one, or a new form replaced it: drop anything prepared. */
    fun discard()
}

private object NoNativeReviewHooks : NativeReviewHooks {
    override fun prefetch() = Unit
    override fun request() = Unit
    override fun discard() = Unit
}

internal fun createFeedbackFormController(
    logger: Logger,
    nativeReviewHooks: NativeReviewHooks = NoNativeReviewHooks,
): FeedbackFormController = FeedbackFormControllerImpl(logger, nativeReviewHooks)

private const val LOGGER_TAG = "[FeedbackFormController]"

private class FeedbackFormControllerImpl(
    private val logger: Logger,
    private val nativeReviewHooks: NativeReviewHooks,
) : FeedbackFormController {
    private val _form = MutableStateFlow<FeedbackForm?>(null)
    override val form: StateFlow<FeedbackForm?> = _form

    private val _isSubmitted = MutableStateFlow<Boolean?>(null)
    override val isSubmitted: StateFlow<Boolean?> = _isSubmitted

    /** Whether the current form collected an answer that asks for the native review. */
    private val nativeReviewRequested = MutableStateFlow(false)

    override fun publish(form: FeedbackForm) {
        var didPublish = false
        _form.update { current ->
            current ?: form.also {
                _isSubmitted.value = false
                didPublish = true
                logger.debug(LOGGER_TAG, "Published form with formId: ${form.formId}")
            }
        }
        if (didPublish) {
            nativeReviewRequested.value = false
            nativeReviewHooks.discard()
        }
    }

    override fun dismiss(): ImpressionId? {
        val impressionId = _form.value?.impressionId
        val isSubmitted = _isSubmitted.value == true
        var didDismiss = false
        _form.update { current ->
            if (current == null) return@update null
            _isSubmitted.value = null
            didDismiss = true
            logger.debug(LOGGER_TAG, "Dismissed form with formId: ${current.formId}")
            null
        }
        // Only the call that actually closed the form may ask, and only once.
        if (didDismiss) {
            if (nativeReviewRequested.getAndUpdate { false }) {
                logger.debug(LOGGER_TAG, "Requesting the native review after the form closed")
                nativeReviewHooks.request()
            } else {
                nativeReviewHooks.discard()
            }
        }
        return impressionId?.takeIf { !isSubmitted }
    }

    override fun submit() {
        _isSubmitted.update { old -> if (old != null) true else old }
    }

    override fun markNativeReviewRequested() {
        if (_form.value == null) return
        if (!nativeReviewRequested.getAndUpdate { true }) {
            nativeReviewHooks.prefetch()
        }
    }
}