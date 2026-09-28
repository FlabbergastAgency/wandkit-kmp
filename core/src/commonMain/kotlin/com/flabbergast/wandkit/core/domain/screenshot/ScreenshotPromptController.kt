package com.flabbergast.wandkit.core.domain.screenshot

import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.FireAndForgetTask
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import com.flabbergast.wandkit.core.replay.ReplayRecording
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/** A captured screenshot moving through the native report flow. */
internal data class ScreenshotPrompt(
    val attachment: WandKitComposerAttachment,
    val phase: Phase = Phase.Prompt,
    /**
     * The session replay frozen right before the card appeared, if the
     * recorder was on and had a frame. Owned by this prompt: the controller
     * discards it when the card goes away, sent or not.
     */
    val replay: ReplayRecording? = null,
    /** The "Include a replay of the last minute" switch. Meaningless without [replay]. */
    val includeReplay: Boolean = replay != null,
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

    /**
     * Ignored while a prompt is already up - in which case the caller still
     * owns [ScreenshotPrompt.replay] and must discard it.
     *
     * @return whether [prompt] was published.
     */
    fun publish(prompt: ScreenshotPrompt): Boolean

    fun dismiss()

    /** Moves from the card to the text composer; the screenshot is already attached. */
    fun report()

    fun updateText(text: String)

    /** The "Include a replay of the last minute" switch; ignored when the prompt has no replay. */
    fun setIncludeReplay(include: Boolean)

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

    override fun publish(prompt: ScreenshotPrompt): Boolean {
        var published = false
        _prompt.update { current ->
            published = current == null
            current ?: prompt
        }
        if (published) {
            logger.debug(
                LOGGER_TAG,
                "Published screenshot prompt (${prompt.attachment.data.size} bytes, replay=${prompt.replay})",
            )
        }
        return published
    }

    override fun dismiss() {
        val dismissed = _prompt.getAndUpdate { null }
        if (dismissed != null) {
            dismissed.replay?.discard()
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

    override fun setIncludeReplay(include: Boolean) {
        _prompt.update { current ->
            if (current?.replay == null) return@update current
            val phase = current.phase
            if (phase is ScreenshotPrompt.Phase.Composing && phase.isSending) return@update current
            current.copy(includeReplay = include)
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

        // Read the switch from the latest state, not `current`: it can have
        // been flipped since the Composing phase began.
        val replayToSend = _prompt.value?.takeIf { it.includeReplay }?.replay

        fireAndForgetTask {
            submitReport(text, current.attachment, replayToSend)
                .onSuccess { postId ->
                    logger.debug(LOGGER_TAG, "Screenshot report sent (postId=$postId)")
                    _prompt.update { updated -> updated?.copy(phase = ScreenshotPrompt.Phase.Sent) }
                    // Uploaded (or left out) - either way the local copy has served its purpose.
                    current.replay?.discard()
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
