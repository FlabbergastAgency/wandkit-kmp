package com.flabbergast.wandkit.core.domain.screenshot

import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.FireAndForgetTask
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import com.flabbergast.wandkit.core.replay.ReplayRecording
import com.flabbergast.wandkit.core.replay.ReplayRecordingSource
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Never actually runs the block - for tests that don't call [ScreenshotPromptController.send]. */
private fun noopFireAndForgetTask(): FireAndForgetTask = object : FireAndForgetTask {
    override fun invoke(block: suspend () -> Unit) = Unit
}

/** Launches the block as a child of the test scope, so `runTest` can drive it with virtual time. */
private fun CoroutineScope.launchingFireAndForgetTask(): FireAndForgetTask = object : FireAndForgetTask {
    override fun invoke(block: suspend () -> Unit) {
        launch { block() }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotPromptControllerTest {
    private fun attachment(name: String = "screenshot") = WandKitComposerAttachment(
        data = byteArrayOf(1, 2, 3),
        contentType = "image/png",
        fileName = "$name.png",
    )

    private val noopTask = noopFireAndForgetTask()

    @Test
    fun publishSetsThePrompt() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        val prompt = ScreenshotPrompt(attachment())

        controller.publish(prompt)

        assertEquals(prompt, controller.prompt.value)
    }

    @Test
    fun secondPublishWhileOneIsUpIsIgnored() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        val first = ScreenshotPrompt(attachment("first"))
        val second = ScreenshotPrompt(attachment("second"))

        controller.publish(first)
        controller.publish(second)

        assertEquals(first, controller.prompt.value)
    }

    @Test
    fun dismissClearsThePrompt() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))

        controller.dismiss()

        assertNull(controller.prompt.value)
    }

    @Test
    fun dismissDuringComposingClearsThePrompt() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))
        controller.report()

        controller.dismiss()

        assertNull(controller.prompt.value)
    }

    @Test
    fun reportMovesFromPromptToComposingWithEmptyText() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))

        controller.report()

        val phase = controller.prompt.value?.phase
        assertIs<ScreenshotPrompt.Phase.Composing>(phase)
        assertEquals("", phase.text)
        assertFalse(phase.isSending)
        assertNull(phase.error)
    }

    @Test
    fun reportWithNothingUpDoesNothing() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )

        controller.report()

        assertNull(controller.prompt.value)
    }

    @Test
    fun updateTextUpdatesComposingTextAndClearsAnExistingError() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))
        controller.report()

        controller.updateText("It crashes when I tap Save")

        val phase = controller.prompt.value?.phase
        assertIs<ScreenshotPrompt.Phase.Composing>(phase)
        assertEquals("It crashes when I tap Save", phase.text)
        assertNull(phase.error)
    }

    @Test
    fun sendWithBlankTextDoesNotCallTheUseCase() {
        var called = false
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> called = true; Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))
        controller.report()
        controller.updateText("   ")

        controller.send()

        assertFalse(called)
        val phase = controller.prompt.value?.phase
        assertIs<ScreenshotPrompt.Phase.Composing>(phase)
        assertFalse(phase.isSending)
    }

    @Test
    fun sendWithNothingUpDoesNothing() {
        var called = false
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> called = true; Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )

        controller.send()

        assertFalse(called)
        assertNull(controller.prompt.value)
    }

    @Test
    fun sendSucceedsThenAutoDismissesAfterShowingSent() = runTest {
        val fireAndForgetTask = launchingFireAndForgetTask()
        val controller = createScreenshotPromptController(
            submitReport = { text, attachment, _ ->
                assertEquals("It crashes when I tap Save", text)
                assertEquals("screenshot.png", attachment.fileName)
                Result.success("post-1")
            },
            fireAndForgetTask = fireAndForgetTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))
        controller.report()
        controller.updateText("It crashes when I tap Save")

        controller.send()
        runCurrent()

        assertEquals(ScreenshotPrompt.Phase.Sent, controller.prompt.value?.phase)

        advanceTimeBy(1300)
        runCurrent()

        assertNull(controller.prompt.value)
    }

    @Test
    fun sendShowsIsSendingWhileInFlight() = runTest {
        val fireAndForgetTask = launchingFireAndForgetTask()
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = fireAndForgetTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))
        controller.report()
        controller.updateText("It crashes")

        controller.send()

        val phase = controller.prompt.value?.phase
        assertIs<ScreenshotPrompt.Phase.Composing>(phase)
        assertEquals(true, phase.isSending)
    }

    @Test
    fun sendFailureSetsErrorAndStaysComposingThenRetrySucceeds() = runTest {
        var attempt = 0
        val fireAndForgetTask = launchingFireAndForgetTask()
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ ->
                attempt += 1
                if (attempt == 1) Result.failure(Exception("network down")) else Result.success("post-1")
            },
            fireAndForgetTask = fireAndForgetTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))
        controller.report()
        controller.updateText("It crashes")

        controller.send()
        runCurrent()

        val failedPhase = controller.prompt.value?.phase
        assertIs<ScreenshotPrompt.Phase.Composing>(failedPhase)
        assertEquals("network down", failedPhase.error)
        assertFalse(failedPhase.isSending)
        assertEquals("It crashes", failedPhase.text)

        controller.send()
        runCurrent()

        assertEquals(ScreenshotPrompt.Phase.Sent, controller.prompt.value?.phase)

        advanceTimeBy(1300)
        runCurrent()

        assertNull(controller.prompt.value)
    }

    // region Session replay

    private fun replay(): Pair<ReplayRecording, TrackingRecordingSource> {
        val source = TrackingRecordingSource()
        return ReplayRecording(source, startEpochMillis = 0, durationMillis = 1000, frameCount = 2, eventCount = 3) to source
    }

    @Test
    fun promptWithReplayIncludesItByDefault() {
        val (recording, _) = replay()
        assertTrue(ScreenshotPrompt(attachment(), replay = recording).includeReplay)
        assertFalse(ScreenshotPrompt(attachment()).includeReplay)
    }

    @Test
    fun secondPublishIsRejectedSoTheCallerCanDiscardItsReplay() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        val (second, _) = replay()

        assertTrue(controller.publish(ScreenshotPrompt(attachment("first"))))
        assertFalse(controller.publish(ScreenshotPrompt(attachment("second"), replay = second)))
    }

    @Test
    fun setIncludeReplayTogglesOnlyWhenThereIsAReplay() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        controller.publish(ScreenshotPrompt(attachment()))
        controller.setIncludeReplay(true)
        assertFalse(controller.prompt.value!!.includeReplay)

        controller.dismiss()
        val (recording, _) = replay()
        controller.publish(ScreenshotPrompt(attachment(), replay = recording))
        controller.report()
        controller.setIncludeReplay(false)
        assertFalse(controller.prompt.value!!.includeReplay)
        controller.setIncludeReplay(true)
        assertTrue(controller.prompt.value!!.includeReplay)
    }

    @Test
    fun sendPassesTheReplayWhenIncludedAndDiscardsItAfterSuccess() = runTest {
        var sentReplay: ReplayRecording? = null
        val controller = createScreenshotPromptController(
            submitReport = { _, _, replay ->
                sentReplay = replay
                Result.success("post-1")
            },
            fireAndForgetTask = launchingFireAndForgetTask(),
            logger = NoOpLogger,
        )
        val (recording, source) = replay()
        controller.publish(ScreenshotPrompt(attachment(), replay = recording))
        controller.report()
        controller.updateText("It crashes")

        controller.send()
        runCurrent()

        assertSame(recording, sentReplay)
        assertTrue(source.discarded)
    }

    @Test
    fun sendLeavesTheReplayOutWhenSwitchedOffButStillDiscardsIt() = runTest {
        var submitted = false
        var sentReplay: ReplayRecording? = null
        val controller = createScreenshotPromptController(
            submitReport = { _, _, replay ->
                submitted = true
                sentReplay = replay
                Result.success("post-1")
            },
            fireAndForgetTask = launchingFireAndForgetTask(),
            logger = NoOpLogger,
        )
        val (recording, source) = replay()
        controller.publish(ScreenshotPrompt(attachment(), replay = recording))
        controller.report()
        controller.setIncludeReplay(false)
        controller.updateText("It crashes")

        controller.send()
        runCurrent()

        assertTrue(submitted)
        assertNull(sentReplay)
        assertTrue(source.discarded)
    }

    @Test
    fun dismissDiscardsTheReplay() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )
        val (recording, source) = replay()
        controller.publish(ScreenshotPrompt(attachment(), replay = recording))

        controller.dismiss()

        assertTrue(source.discarded)
    }

    @Test
    fun failedSendKeepsTheReplayForTheRetry() = runTest {
        val controller = createScreenshotPromptController(
            submitReport = { _, _, _ -> Result.failure(Exception("network down")) },
            fireAndForgetTask = launchingFireAndForgetTask(),
            logger = NoOpLogger,
        )
        val (recording, source) = replay()
        controller.publish(ScreenshotPrompt(attachment(), replay = recording))
        controller.report()
        controller.updateText("It crashes")

        controller.send()
        runCurrent()

        assertFalse(source.discarded)
        assertSame(recording, controller.prompt.value?.replay)
    }

    // endregion
}

private class TrackingRecordingSource : ReplayRecordingSource {
    var discarded = false
        private set

    override val sizeBytes: Long = 10

    override fun readBytes(): ByteArray? = if (discarded) null else ByteArray(10)

    override fun discard() {
        discarded = true
    }
}

