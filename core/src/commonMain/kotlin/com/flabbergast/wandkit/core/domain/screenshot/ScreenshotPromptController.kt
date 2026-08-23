package com.flabbergast.wandkit.core.domain.screenshot

import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.FireAndForgetTask
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/** A captured screenshot moving through the native report flow. */
internal data class ScreenshotPrompt(
    val attachment: WandKitComposerAttachment,
    val phase: Phase = Phase.Prompt,
) {
    internal sealed interface Phase {
        /** The "Report a problem?" card. */
        data object Prompt : Phase

        /** The text box, screenshot already attached. */
        data class Composing(
            val text: String = "",
            val isSending: Boolean = false,
            val error: String? = null,
        ) : Phase

        /** The thank-you state; the controller auto-dismisses out of this. */
        data object Sent : Phase
    }
}

/**
 * The bridge between the platform screenshot detector (which publishes) and
 * the UI (which renders the card, collects the report text and sends it), in
 * the same shape as `FeedbackFormController`.
 *
 * [prompt] stays non-null for the entire flow - the root Decompose slot is
 * gated on it, so clearing it early would drop the card mid-flow.
 */
internal interface ScreenshotPromptController {
    val prompt: StateFlow<ScreenshotPrompt?>

    /** Ignored while a prompt is already up. */
    fun publish(prompt: ScreenshotPrompt)

    fun dismiss()

    /** Moves from the card to the text composer; the screenshot is already attached. */
    fun report()

    fun updateText(text: String)

    /** Uploads the screenshot and creates the report post; auto-dismisses a moment after success. */
    fun send()
}

internal fun createScreenshotPromptController(
    submitReport: SubmitScreenshotReportUseCase,
    fireAndForgetTask: FireAndForgetTask,
    logger: Logger,
): ScreenshotPromptController = ScreenshotPromptControllerImpl(
    submitReport = submitReport,
    fireAndForgetTask = fireAndForgetTask,
    logger = logger,
)

private const val LOGGER_TAG = "[ScreenshotPromptController]"
private const val SENT_AUTO_DISMISS_MILLIS = 1200L
private const val DEFAULT_ERROR_MESSAGE = "Couldn't send that. Please try again."

private class ScreenshotPromptControllerImpl(
    private val submitReport: SubmitScreenshotReportUseCase,
    private val fireAndForgetTask: FireAndForgetTask,
    private val logger: Logger,
) : ScreenshotPromptController {
    private val _prompt = MutableStateFlow<ScreenshotPrompt?>(null)
    override val prompt: StateFlow<ScreenshotPrompt?> = _prompt

    override fun publish(prompt: ScreenshotPrompt) {
        _prompt.update { current ->
            current ?: prompt.also {
                logger.debug(LOGGER_TAG, "Published screenshot prompt (${it.attachment.data.size} bytes)")
            }
        }
    }

    override fun dismiss() {
        if (_prompt.getAndUpdate { null } != null) {
            logger.debug(LOGGER_TAG, "Dismissed screenshot prompt")
        }
    }

    override fun report() {
        var reported = false
        _prompt.update { current ->
            if (current == null) return@update null
            reported = true
            current.copy(phase = ScreenshotPrompt.Phase.Composing())
        }
        if (reported) {
            logger.debug(LOGGER_TAG, "Reporting screenshot")
        }
    }

    override fun updateText(text: String) {
        _prompt.update { current ->
            val phase = current?.phase as? ScreenshotPrompt.Phase.Composing ?: return@update current
            current.copy(phase = phase.copy(text = text, error = null))
        }
    }

    override fun send() {
        val current = _prompt.value ?: return
        val composing = current.phase as? ScreenshotPrompt.Phase.Composing ?: return
        val text = composing.text.trim()
        if (text.isEmpty()) return

        _prompt.update { updated ->
            val phase = updated?.phase as? ScreenshotPrompt.Phase.Composing ?: return@update updated
            updated.copy(phase = phase.copy(isSending = true, error = null))
        }

        fireAndForgetTask {
            submitReport(text, current.attachment)
                .onSuccess { postId ->
                    logger.debug(LOGGER_TAG, "Screenshot report sent (postId=$postId)")
                    _prompt.update { updated -> updated?.copy(phase = ScreenshotPrompt.Phase.Sent) }
                    delay(SENT_AUTO_DISMISS_MILLIS)
                    _prompt.update { updated -> if (updated?.phase == ScreenshotPrompt.Phase.Sent) null else updated }
                }
                .onFailure { error ->
                    logger.debug(LOGGER_TAG, "Screenshot report failed: $error")
                    _prompt.update { updated ->
                        val phase = updated?.phase as? ScreenshotPrompt.Phase.Composing ?: return@update updated
                        updated.copy(phase = phase.copy(isSending = false, error = error.message ?: DEFAULT_ERROR_MESSAGE))
                    }
                }
        }
    }
}
