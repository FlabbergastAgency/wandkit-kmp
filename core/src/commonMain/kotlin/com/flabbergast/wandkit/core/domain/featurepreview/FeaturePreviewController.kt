package com.flabbergast.wandkit.core.domain.featurepreview

import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.FireAndForgetTask
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.featurepreview.WandKitFeaturePreviewResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/** A feature-preview sheet's native state, published once the post's feature-preview state is known. */
internal data class FeaturePreviewPrompt(
    val postId: String,
    /** Dashboard-managed copy for the sheet's current [state] - the endpoint returns only the copy for that state. */
    val previewCopy: FeaturePreviewCopy,
    val state: State,
) {
    internal sealed interface State {
        data class ComingSoon(
            val viewerVoted: Boolean,
            val isVoting: Boolean = false,
            val error: String? = null,
        ) : State

        data class Available(
            /** `null` hides the primary button - no store URL configured for this project/platform. */
            val storeUrl: String?,
        ) : State

        /**
         * Generic "coming soon" fallback: no preview configured for the
         * post, disabled, unpublished, or the fetch (including the session
         * mint) failed. Single primary action, no secondary - Close just
         * dismisses.
         */
        data object Generic : State
    }
}

/**
 * The bridge between [com.flabbergast.wandkit.core.featurepreview.presentFeaturePreviewFlow]
 * (which resolves the post's feature-preview state and publishes) and the UI
 * (which renders the sheet and forwards taps), in the same shape as
 * `ScreenshotPromptController`.
 *
 * [prompt] stays non-null for the entire flow - the root Decompose slot is
 * gated on it, so clearing it early would drop the sheet mid-flow.
 *
 * No push-permission prompt here: the native WandKit iOS SDK asks for
 * notification permission right after a successful subscribe, but this KMP
 * SDK has no push device registration or `POST_NOTIFICATIONS` handling yet
 * on Android, so asking now would grant nothing deliverable. Add the same
 * prompt to [onPrimary]'s success path once push registration exists.
 */
internal interface FeaturePreviewController {
    val prompt: StateFlow<FeaturePreviewPrompt?>

    /** Ignored while a prompt is already up; the caller's [onResult] fires with [WandKitFeaturePreviewResult.Dismissed] so it is never left hanging. */
    fun publish(prompt: FeaturePreviewPrompt, onResult: (WandKitFeaturePreviewResult) -> Unit)

    /**
     * Coming-soon primary: votes (auto-follows) unless the viewer already has,
     * then dismisses the sheet, opens the feedback screen on the post with a
     * one-time confirmation, and reports [WandKitFeaturePreviewResult.Subscribed].
     * No-op once mid-vote, or once the sheet has nothing to show.
     */
    fun onPrimary()

    /** Available primary: the view has already opened the store URL - this just concludes the flow. */
    fun onUpdateApp()

    /** Secondary tap: the host continues its own flow (e.g. "Continue with EASA"). */
    fun onSecondary()

    /** Backdrop tap, or back press. */
    fun onDismiss()
}

internal fun createFeaturePreviewController(
    voteUseCase: VoteFeaturePreviewPostUseCase,
    recordEvent: suspend (name: String, properties: Map<String, String>) -> Unit,
    openFeedbackPost: (postId: String) -> Unit,
    fireAndForgetTask: FireAndForgetTask,
    logger: Logger,
): FeaturePreviewController = FeaturePreviewControllerImpl(
    voteUseCase = voteUseCase,
    recordEvent = recordEvent,
    openFeedbackPost = openFeedbackPost,
    fireAndForgetTask = fireAndForgetTask,
    logger = logger,
)

private const val LOGGER_TAG = "[FeaturePreviewController]"
private const val DEFAULT_VOTE_ERROR_MESSAGE = "Couldn't save, please try again"
internal const val FEATURE_PREVIEW_FOLLOWED_EVENT = "feature_preview_followed"
internal const val FEATURE_PREVIEW_SECONDARY_EVENT = "feature_preview_secondary"

/** Appended to the feedback post URL so the web app shows a one-time "You're following this request" notice. */
internal const val FEATURE_PREVIEW_FOLLOWED_QUERY = "followed=1"

private class FeaturePreviewControllerImpl(
    private val voteUseCase: VoteFeaturePreviewPostUseCase,
    private val recordEvent: suspend (name: String, properties: Map<String, String>) -> Unit,
    private val openFeedbackPost: (postId: String) -> Unit,
    private val fireAndForgetTask: FireAndForgetTask,
    private val logger: Logger,
) : FeaturePreviewController {
    private val _prompt = MutableStateFlow<FeaturePreviewPrompt?>(null)
    override val prompt: StateFlow<FeaturePreviewPrompt?> = _prompt

    private var onResult: ((WandKitFeaturePreviewResult) -> Unit)? = null

    override fun publish(prompt: FeaturePreviewPrompt, onResult: (WandKitFeaturePreviewResult) -> Unit) {
        var accepted = false
        _prompt.update { current ->
            if (current != null) {
                current
            } else {
                accepted = true
                prompt
            }
        }

        if (accepted) {
            this.onResult = onResult
            logger.debug(LOGGER_TAG, "Published feature preview prompt (postId=${prompt.postId})")
        } else {
            logger.debug(LOGGER_TAG, "Ignored feature preview publish: one is already up")
            onResult(WandKitFeaturePreviewResult.Dismissed)
        }
    }

    override fun onPrimary() {
        val current = _prompt.value ?: return
        val comingSoon = current.state as? FeaturePreviewPrompt.State.ComingSoon ?: return
        if (comingSoon.isVoting) return

        if (comingSoon.viewerVoted) {
            completeSubscribed(current.postId)
            return
        }

        updateComingSoon { it.copy(isVoting = true, error = null) }

        fireAndForgetTask {
            voteUseCase(current.postId)
                .onSuccess {
                    logger.debug(LOGGER_TAG, "Feature preview vote succeeded (postId=${current.postId})")
                    runCatching { recordEvent(FEATURE_PREVIEW_FOLLOWED_EVENT, mapOf("post_id" to current.postId)) }
                        .onFailure { logger.warn(LOGGER_TAG, "Failed to record $FEATURE_PREVIEW_FOLLOWED_EVENT", it) }

                    completeSubscribed(current.postId)
                }
                .onFailure { error ->
                    logger.warn(LOGGER_TAG, "Feature preview vote failed (postId=${current.postId})", error)
                    updateComingSoon { it.copy(isVoting = false, error = DEFAULT_VOTE_ERROR_MESSAGE) }
                }
        }
    }

    override fun onUpdateApp() {
        if (_prompt.getAndUpdate { null } != null) {
            onResult?.invoke(WandKitFeaturePreviewResult.UpdateApp)
            onResult = null
        }
    }

    override fun onSecondary() {
        val previous = _prompt.getAndUpdate { null } ?: return

        fireAndForgetTask {
            runCatching { recordEvent(FEATURE_PREVIEW_SECONDARY_EVENT, mapOf("post_id" to previous.postId)) }
                .onFailure { logger.warn(LOGGER_TAG, "Failed to record $FEATURE_PREVIEW_SECONDARY_EVENT", it) }
        }

        onResult?.invoke(WandKitFeaturePreviewResult.Secondary)
        onResult = null
    }

    override fun onDismiss() {
        if (_prompt.getAndUpdate { null } != null) {
            onResult?.invoke(WandKitFeaturePreviewResult.Dismissed)
            onResult = null
        }
    }

    /** Dismisses the sheet, opens the feedback post with the follow confirmation, and reports Subscribed - shared by the freshly-voted and already-voted paths. */
    private fun completeSubscribed(postId: String) {
        if (_prompt.getAndUpdate { null } != null) {
            openFeedbackPost(postId)
            onResult?.invoke(WandKitFeaturePreviewResult.Subscribed)
            onResult = null
        }
    }

    private fun updateComingSoon(
        transform: (FeaturePreviewPrompt.State.ComingSoon) -> FeaturePreviewPrompt.State.ComingSoon,
    ) {
        _prompt.update { updated ->
            val state = updated?.state as? FeaturePreviewPrompt.State.ComingSoon ?: return@update updated
            updated.copy(state = transform(state))
        }
    }
}
