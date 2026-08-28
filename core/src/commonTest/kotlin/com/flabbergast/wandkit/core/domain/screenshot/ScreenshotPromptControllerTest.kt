package com.flabbergast.wandkit.core.domain.screenshot

import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.FireAndForgetTask
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
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
            submitReport = { _, _ -> Result.success("post-1") },
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
            submitReport = { _, _ -> Result.success("post-1") },
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
            submitReport = { _, _ -> Result.success("post-1") },
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
            submitReport = { _, _ -> Result.success("post-1") },
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
            submitReport = { _, _ -> Result.success("post-1") },
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
            submitReport = { _, _ -> Result.success("post-1") },
            fireAndForgetTask = noopTask,
            logger = NoOpLogger,
        )

        controller.report()

        assertNull(controller.prompt.value)
    }

    @Test
    fun updateTextUpdatesComposingTextAndClearsAnExistingError() {
        val controller = createScreenshotPromptController(
            submitReport = { _, _ -> Result.success("post-1") },
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
            submitReport = { _, _ -> called = true; Result.success("post-1") },
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
            submitReport = { _, _ -> called = true; Result.success("post-1") },
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
            submitReport = { text, attachment ->
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
            submitReport = { _, _ -> Result.success("post-1") },
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
            submitReport = { _, _ ->
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
}

private object NoOpLogger : Logger {
    override fun verbose(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun debug(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun info(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun warn(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun error(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun assert(tag: String?, message: String, throwable: Throwable?) = Unit
}
