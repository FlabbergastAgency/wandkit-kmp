package com.flabbergast.wandkit.core.review

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val EXIT_DELAY = 310L

private class FakeTimer : LaunchTimer {
    override var nowMillis = 0L
        private set

    private var dueAt: Long? = null
    private var action: (() -> Unit)? = null

    override fun schedule(delayMillis: Long, action: () -> Unit) {
        dueAt = nowMillis + delayMillis
        this.action = action
    }

    override fun cancel() {
        dueAt = null
        action = null
    }

    fun advanceBy(millis: Long) {
        nowMillis += millis
        val due = dueAt ?: return
        if (due <= nowMillis) {
            val run = action
            cancel()
            run?.invoke()
        }
    }
}

private class FakeClient : ReviewFlowClient<String> {
    var fetches = 0
    val launched = mutableListOf<String>()
    var skipReason: String? = null
    private var onResult: ((Result<String>) -> Unit)? = null

    override fun requestReviewFlow(onResult: (Result<String>) -> Unit) {
        fetches++
        this.onResult = onResult
    }

    override fun launchReviewFlow(info: String): String? {
        skipReason?.let { return it }
        launched += info
        return null
    }

    fun succeed(info: String = "info") = onResult!!(Result.success(info))
    fun fail() = onResult!!(Result.failure(RuntimeException("Play said no")))
}

private class RecordingLogger : Logger {
    val messages = mutableListOf<String>()
    override fun verbose(tag: String?, message: String, throwable: Throwable?) { messages += message }
    override fun debug(tag: String?, message: String, throwable: Throwable?) { messages += message }
    override fun info(tag: String?, message: String, throwable: Throwable?) { messages += message }
    override fun warn(tag: String?, message: String, throwable: Throwable?) { messages += message }
    override fun error(tag: String?, message: String, throwable: Throwable?) { messages += message }
    override fun assert(tag: String?, message: String, throwable: Throwable?) { messages += message }
}

class NativeReviewCoordinatorTest {
    private val timer = FakeTimer()
    private val logger = RecordingLogger()
    private val coordinator = NativeReviewCoordinator<String>(timer, EXIT_DELAY)
    private val clients = mutableListOf<FakeClient>()

    private fun prefetch(): FakeClient? {
        var created: FakeClient? = null
        coordinator.prefetch(logger) { FakeClient().also { created = it; clients += it } }
        return created
    }

    private val launched get() = clients.flatMap { it.launched }

    // region Fetch and close ordering

    @Test
    fun fetchedBeforeCloseLaunchesOnceTheExitAnimationIsOver() {
        val client = prefetch()!!
        client.succeed()

        coordinator.request(logger)
        timer.advanceBy(EXIT_DELAY - 1)
        assertEquals(emptyList(), launched)

        timer.advanceBy(1)
        assertEquals(listOf("info"), launched)
    }

    @Test
    fun fetchFinishingAfterTheAnimationLaunchesRightAway() {
        val client = prefetch()!!
        coordinator.request(logger)
        timer.advanceBy(1_000)
        assertEquals(emptyList(), launched)

        client.succeed()
        timer.advanceBy(0)

        assertEquals(listOf("info"), launched)
    }

    @Test
    fun fetchFinishingMidAnimationStillWaitsForItToEnd() {
        val client = prefetch()!!
        coordinator.request(logger)
        timer.advanceBy(100)

        client.succeed()
        timer.advanceBy(EXIT_DELAY - 101)
        assertEquals(emptyList(), launched)

        timer.advanceBy(1)
        assertEquals(listOf("info"), launched)
    }

    @Test
    fun noLaunchWhileTheFormIsStillUp() {
        prefetch()!!.succeed()

        timer.advanceBy(10_000)

        assertEquals(emptyList(), launched)
    }

    // endregion

    // region Fetching

    @Test
    fun prefetchFetchesOnlyOnce() {
        val client = prefetch()!!
        prefetch()
        client.succeed()
        prefetch()

        assertEquals(1, clients.size)
        assertEquals(1, client.fetches)
    }

    @Test
    fun failedFetchSkipsTheLaunchAndWarns() {
        val client = prefetch()!!
        coordinator.request(logger)

        client.fail()
        timer.advanceBy(10_000)

        assertEquals(emptyList(), launched)
        assertTrue(logger.messages.any { it.startsWith("Couldn't fetch the review info") })
    }

    @Test
    fun unavailableClientMeansNoReview() {
        coordinator.prefetch(logger) { null }
        coordinator.request(logger)
        timer.advanceBy(10_000)

        assertTrue(logger.messages.any { it.startsWith("No review info fetched") })
    }

    @Test
    fun requestWithoutPrefetchDoesNothing() {
        coordinator.request(logger)
        timer.advanceBy(10_000)

        assertEquals(emptyList(), launched)
    }

    // endregion

    // region Discarding and single use

    @Test
    fun discardDropsAFetchStillInFlight() {
        val staleClient = prefetch()!!
        coordinator.discard()
        coordinator.request(logger)

        staleClient.succeed()
        timer.advanceBy(10_000)

        assertEquals(emptyList(), launched)
    }

    @Test
    fun discardCancelsAScheduledLaunch() {
        prefetch()!!.succeed()
        coordinator.request(logger)
        timer.advanceBy(100)

        coordinator.discard()
        timer.advanceBy(10_000)

        assertEquals(emptyList(), launched)
    }

    @Test
    fun aStaleFetchDoesNotLeakIntoTheNextForm() {
        val staleClient = prefetch()!!
        coordinator.discard()
        val freshClient = prefetch()!!

        staleClient.succeed("stale")
        freshClient.succeed("fresh")
        coordinator.request(logger)
        timer.advanceBy(EXIT_DELAY)

        assertEquals(listOf("fresh"), launched)
    }

    @Test
    fun reviewInfoIsSingleUse() {
        prefetch()!!.succeed()
        coordinator.request(logger)
        timer.advanceBy(EXIT_DELAY)

        coordinator.request(logger)
        timer.advanceBy(EXIT_DELAY)

        assertEquals(listOf("info"), launched)
    }

    @Test
    fun aSkippedLaunchLogsWhyAndIsNotRetried() {
        val client = prefetch()!!
        client.skipReason = "the invite gate is up"
        client.succeed()
        coordinator.request(logger)
        timer.advanceBy(EXIT_DELAY)

        client.skipReason = null
        timer.advanceBy(10_000)

        assertEquals(emptyList(), launched)
        assertTrue(logger.messages.contains("Skipping the native review: the invite gate is up"))
    }

    // endregion
}
