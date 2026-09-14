package com.flabbergast.wandkit.core.domain.featurepreview

import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.FireAndForgetTask
import com.flabbergast.wandkit.core.featurepreview.WandKitFeaturePreviewResult
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Never actually runs the block - for tests that don't call [FeaturePreviewController.onPrimary]. */
private fun noopFireAndForgetTask(): FireAndForgetTask = object : FireAndForgetTask {
    override fun invoke(block: suspend () -> Unit) = Unit
}

/** Launches the block as a child of the test scope, so `runTest` can drive it with virtual time. */
private fun CoroutineScope.launchingFireAndForgetTask(): FireAndForgetTask = object : FireAndForgetTask {
    override fun invoke(block: suspend () -> Unit) {
        launch { block() }
    }
}

private val previewCopy = FeaturePreviewCopy(
    title = "FAA support is almost here",
    message = "Coming soon.",
    primaryLabel = "Let me know",
    secondaryLabel = "Continue with EASA",
)

private fun comingSoonPrompt(postId: String = "post-1", viewerVoted: Boolean = false) = FeaturePreviewPrompt(
    postId = postId,
    previewCopy = previewCopy,
    state = FeaturePreviewPrompt.State.ComingSoon(viewerVoted = viewerVoted),
)

private fun availablePrompt(postId: String = "post-1", storeUrl: String? = "https://play.google.com/x") = FeaturePreviewPrompt(
    postId = postId,
    previewCopy = previewCopy,
    state = FeaturePreviewPrompt.State.Available(storeUrl = storeUrl),
)

private fun genericPrompt(postId: String = "post-1") = FeaturePreviewPrompt(
    postId = postId,
    previewCopy = previewCopy,
    state = FeaturePreviewPrompt.State.Generic,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FeaturePreviewControllerTest {
    private val noopTask = noopFireAndForgetTask()

    private fun controller(
        voteUseCase: VoteFeaturePreviewPostUseCase = { Result.success(Unit) },
        recordEvent: suspend (String, Map<String, String>) -> Unit = { _, _ -> },
        openFeedbackPost: (String) -> Unit = {},
        fireAndForgetTask: FireAndForgetTask = noopTask,
    ) = createFeaturePreviewController(
        voteUseCase = voteUseCase,
        recordEvent = recordEvent,
        openFeedbackPost = openFeedbackPost,
        fireAndForgetTask = fireAndForgetTask,
        logger = NoOpLogger,
    )

    @Test
    fun publishSetsThePrompt() {
        val controller = controller()

        controller.publish(comingSoonPrompt()) {}

        assertEquals(comingSoonPrompt(), controller.prompt.value)
    }

    @Test
    fun secondPublishWhileOneIsUpIsIgnoredAndResolvesAsDismissed() {
        val controller = controller()
        var secondResult: WandKitFeaturePreviewResult? = null

        controller.publish(comingSoonPrompt(postId = "post-1")) {}
        controller.publish(comingSoonPrompt(postId = "post-2")) { secondResult = it }

        assertEquals("post-1", controller.prompt.value?.postId)
        assertEquals(WandKitFeaturePreviewResult.Dismissed, secondResult)
    }

    @Test
    fun onDismissClearsThePromptAndReportsDismissed() {
        val controller = controller()
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(comingSoonPrompt()) { result = it }

        controller.onDismiss()

        assertNull(controller.prompt.value)
        assertEquals(WandKitFeaturePreviewResult.Dismissed, result)
    }

    @Test
    fun genericPromptPublishesSetsThePrompt() {
        val controller = controller()

        controller.publish(genericPrompt()) {}

        assertEquals(genericPrompt(), controller.prompt.value)
    }

    @Test
    fun closeOnGenericPromptReportsDismissed() {
        // The view routes the generic sheet's Close action straight to onDismiss() -
        // see DefaultFeaturePreviewComponent.onPrimary()'s State.Generic branch.
        val controller = controller()
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(genericPrompt()) { result = it }

        controller.onDismiss()

        assertNull(controller.prompt.value)
        assertEquals(WandKitFeaturePreviewResult.Dismissed, result)
    }

    @Test
    fun onPrimaryWithGenericPromptNeverVotes() {
        // Generic has no ComingSoon state to vote on - onPrimary() no-ops
        // rather than reaching the vote use case or fireAndForgetTask at all.
        var voteCalls = 0
        val controller = controller(voteUseCase = { voteCalls++; Result.success(Unit) })
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(genericPrompt()) { result = it }

        controller.onPrimary()

        assertEquals(0, voteCalls)
        assertEquals(genericPrompt(), controller.prompt.value)
        assertNull(result)
    }

    @Test
    fun onSecondaryClearsThePromptReportsSecondaryAndRecordsEvent() = runTest {
        var recordedEvent: String? = null
        var recordedPostId: String? = null
        val controller = controller(
            recordEvent = { name, properties -> recordedEvent = name; recordedPostId = properties["post_id"] },
            fireAndForgetTask = launchingFireAndForgetTask(),
        )
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(comingSoonPrompt(postId = "post-1")) { result = it }

        controller.onSecondary()
        runCurrent()

        assertNull(controller.prompt.value)
        assertEquals(WandKitFeaturePreviewResult.Secondary, result)
        assertEquals(FEATURE_PREVIEW_SECONDARY_EVENT, recordedEvent)
        assertEquals("post-1", recordedPostId)
    }

    @Test
    fun onUpdateAppClearsThePromptAndReportsUpdateApp() {
        val controller = controller()
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(availablePrompt()) { result = it }

        controller.onUpdateApp()

        assertNull(controller.prompt.value)
        assertEquals(WandKitFeaturePreviewResult.UpdateApp, result)
    }

    @Test
    fun onPrimaryVotesThenOpensTheFeedbackPostAndReportsSubscribed() = runTest {
        var voteCalls = 0
        var recordedEvent: String? = null
        var openedPostId: String? = null
        val controller = controller(
            voteUseCase = { voteCalls++; Result.success(Unit) },
            recordEvent = { name, _ -> recordedEvent = name },
            openFeedbackPost = { postId -> openedPostId = postId },
            fireAndForgetTask = launchingFireAndForgetTask(),
        )
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(comingSoonPrompt(postId = "post-1")) { result = it }

        controller.onPrimary()
        runCurrent()

        assertEquals(1, voteCalls)
        assertEquals(FEATURE_PREVIEW_FOLLOWED_EVENT, recordedEvent)
        assertEquals("post-1", openedPostId)
        assertNull(controller.prompt.value)
        assertEquals(WandKitFeaturePreviewResult.Subscribed, result)
    }

    @Test
    fun onPrimarySkipsVoteWhenAlreadyVotedButStillOpensTheFeedbackPost() = runTest {
        var voteCalls = 0
        var openedPostId: String? = null
        val controller = controller(
            voteUseCase = { voteCalls++; Result.success(Unit) },
            openFeedbackPost = { postId -> openedPostId = postId },
            fireAndForgetTask = launchingFireAndForgetTask(),
        )
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(comingSoonPrompt(postId = "post-1", viewerVoted = true)) { result = it }

        controller.onPrimary()
        runCurrent()

        assertEquals(0, voteCalls)
        assertEquals("post-1", openedPostId)
        assertNull(controller.prompt.value)
        assertEquals(WandKitFeaturePreviewResult.Subscribed, result)
    }

    @Test
    fun onPrimaryFailureReEnablesPrimaryAndShowsAnErrorWithoutOpeningFeedback() = runTest {
        var openedPostId: String? = null
        val controller = controller(
            voteUseCase = { Result.failure(RuntimeException("boom")) },
            openFeedbackPost = { postId -> openedPostId = postId },
            fireAndForgetTask = launchingFireAndForgetTask(),
        )
        var result: WandKitFeaturePreviewResult? = null
        controller.publish(comingSoonPrompt()) { result = it }

        controller.onPrimary()
        runCurrent()

        val state = assertIs<FeaturePreviewPrompt.State.ComingSoon>(controller.prompt.value?.state)
        assertFalse(state.isVoting)
        assertEquals("Couldn't save, please try again", state.error)
        assertNull(openedPostId)
        assertNull(result)
    }
}
