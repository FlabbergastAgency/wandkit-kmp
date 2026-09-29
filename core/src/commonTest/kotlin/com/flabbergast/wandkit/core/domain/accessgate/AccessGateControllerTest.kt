package com.flabbergast.wandkit.core.domain.accessgate

import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateBlockReason
import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateState
import com.flabbergast.wandkit.core.accessgate.WandKitAccessPass
import com.flabbergast.wandkit.core.config.WandKitAccessGateOptions
import com.flabbergast.wandkit.core.data.accessgate.AccessGateStore
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val CACHED_PASS = WandKitAccessPass(
    code = "K7QM-2XFT",
    claimCount = 3,
    claimedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
)

private fun status(
    enabled: Boolean = true,
    code: AccessGateCodeStatus? = null,
    title: String? = null,
    message: String? = null,
    helpUrl: String? = null,
) = AccessGateStatus(
    enabled = enabled,
    title = title,
    message = message,
    helpUrl = helpUrl,
    code = code?.let { AccessGateCodeCheck(status = it, claimCount = 3) },
)

private class FakeRepository : AccessGateRepository {
    var onStatus: suspend (String?) -> Result<AccessGateStatus> = { Result.success(status(enabled = true)) }
    var onClaim: suspend (String) -> Result<AccessGateClaim> = { Result.success(AccessGateClaim("K7QM-2XFT", 4)) }
    val statusCalls = mutableListOf<String?>()
    val claimCalls = mutableListOf<String>()

    override suspend fun status(code: String?): Result<AccessGateStatus> {
        statusCalls += code
        return onStatus(code)
    }

    override suspend fun claim(code: String): Result<AccessGateClaim> {
        claimCalls += code
        return onClaim(code)
    }
}

private class FakeStore(
    override var pass: WandKitAccessPass? = null,
    override var lastKnownEnabled: Boolean? = null,
) : AccessGateStore

private class RecordingPresenter : AccessGatePresenter {
    val calls = mutableListOf<String>()
    override fun present() {
        calls += "present"
    }

    override fun dismiss() {
        calls += "dismiss"
    }
}

private class RecordingLogger : Logger {
    val errors = mutableListOf<String>()
    override fun verbose(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun debug(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun info(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun warn(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun error(tag: String?, message: String, throwable: Throwable?) {
        errors += message
    }
    override fun assert(tag: String?, message: String, throwable: Throwable?) = Unit
}

private class TestClock(var now: Instant = Instant.fromEpochMilliseconds(1_800_000_000_000)) : Clock {
    override fun now(): Instant = now
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccessGateControllerTest {
    private val repository = FakeRepository()
    private val store = FakeStore()
    private val presenter = RecordingPresenter()
    private val clock = TestClock()
    private val callbacks = mutableListOf<WandKitAccessGateState>()
    private val options = WandKitAccessGateOptions(onStateChange = { callbacks += it })

    /**
     * An unconfined dispatcher on the test's scheduler: controller work runs
     * inline like `Dispatchers.Main.immediate` does on the main thread, and
     * the timeouts run on virtual time.
     */
    private val misconfigurationLogger = RecordingLogger()

    private fun TestScope.controller(
        isStorePersistent: Boolean = true,
        presenter: AccessGatePresenter? = this@AccessGateControllerTest.presenter,
        onPresenterFactory: () -> Unit = {},
    ) = AccessGateController(
        repository = repository,
        store = store,
        presenterFactory = { onPresenterFactory(); presenter },
        isStorePersistent = isStorePersistent,
        logger = NoOpLogger,
        misconfigurationLogger = misconfigurationLogger,
        dispatcher = UnconfinedTestDispatcher(testScheduler),
        clock = clock,
    )

    private fun blocked(reason: WandKitAccessGateBlockReason) = WandKitAccessGateState.Blocked(reason)

    // Opt-in and environment

    @Test
    fun noOptions_neverCallsNetwork_andStaysDisabled() = runTest {
        var presenterResolved = false
        val controller = controller(onPresenterFactory = { presenterResolved = true })

        controller.start(null)
        advanceTimeBy(1.minutes)

        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertTrue(repository.statusCalls.isEmpty())
        assertFalse(presenterResolved)
        assertTrue(presenter.calls.isEmpty())
    }

    @Test
    fun inMemoryStoreFallback_staysDisabled_withoutNetwork() = runTest {
        store.pass = CACHED_PASS
        val controller = controller(isStorePersistent = false)

        controller.start(options)

        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertTrue(repository.statusCalls.isEmpty())
        assertTrue(callbacks.isEmpty())
    }

    @Test
    fun misconfiguration_isLoggedThroughTheAlwaysOnLogger_evenWithDebugLoggingOff() = runTest {
        controller(isStorePersistent = false).start(options)
        controller(presenter = null).start(options)
        repository.onStatus = { Result.failure(AccessGateApiException(401, "invalid_api_key")) }
        controller().start(options)

        // The regular logger is a no-op here, like a release build's.
        assertEquals(3, misconfigurationLogger.errors.size, misconfigurationLogger.errors.toString())
        assertTrue(misconfigurationLogger.errors[0].contains("configured without a Context"))
        assertTrue(misconfigurationLogger.errors[1].contains("no gate screen"))
        assertTrue(misconfigurationLogger.errors[2].contains("HTTP 401"))
    }

    @Test
    fun noGateScreen_staysDisabled_withoutNetwork() = runTest {
        val controller = controller(presenter = null)

        controller.start(options)

        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertTrue(repository.statusCalls.isEmpty())
    }

    // Launch table: cached pass

    @Test
    fun cachedPass_isPassedSynchronously_beforeTheCheckReturns() = runTest {
        store.pass = CACHED_PASS
        val response = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { response.await() }
        val controller = controller()

        controller.start(options)

        assertEquals(WandKitAccessGateState.Passed(CACHED_PASS), controller.state.value)
        assertEquals(listOf<String?>(CACHED_PASS.code), repository.statusCalls)
        assertEquals(listOf<WandKitAccessGateState>(WandKitAccessGateState.Passed(CACHED_PASS)), callbacks)
        assertEquals(listOf("dismiss"), presenter.calls)
    }

    @Test
    fun cachedPass_serverDisabled_movesToDisabled_andKeepsThePass() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.success(status(enabled = false)) }
        val controller = controller()

        controller.start(options)

        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertEquals(CACHED_PASS, store.pass)
        assertEquals(false, store.lastKnownEnabled)
        assertEquals(listOf(WandKitAccessGateState.Passed(CACHED_PASS), WandKitAccessGateState.Disabled), callbacks)
    }

    @Test
    fun cachedPass_codeActive_staysPassed() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.success(status(code = AccessGateCodeStatus.ACTIVE)) }
        val controller = controller()

        controller.start(options)

        assertEquals(WandKitAccessGateState.Passed(CACHED_PASS), controller.state.value)
        assertEquals(CACHED_PASS, store.pass)
        assertEquals(true, store.lastKnownEnabled)
        assertEquals(1, callbacks.size)
    }

    @Test
    fun cachedPass_codeRevoked_clearsThePass_andBlocks() = runTest {
        assertCachedPassBlocks(AccessGateCodeStatus.REVOKED, WandKitAccessGateBlockReason.Revoked)
    }

    @Test
    fun cachedPass_codeExpired_clearsThePass_andBlocks() = runTest {
        assertCachedPassBlocks(AccessGateCodeStatus.EXPIRED, WandKitAccessGateBlockReason.Expired)
    }

    @Test
    fun cachedPass_codeNotFound_clearsThePass_andBlocks() = runTest {
        assertCachedPassBlocks(AccessGateCodeStatus.NOT_FOUND, WandKitAccessGateBlockReason.NotFound)
    }

    @Test
    fun cachedPass_enabledWithoutCodeStatus_failsClosedAsNotFound() = runTest {
        assertCachedPassBlocks(codeStatus = null, expectedReason = WandKitAccessGateBlockReason.NotFound)
    }

    private fun TestScope.assertCachedPassBlocks(
        codeStatus: AccessGateCodeStatus?,
        expectedReason: WandKitAccessGateBlockReason,
    ) {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.success(status(code = codeStatus)) }
        val controller = controller()

        controller.start(options)

        assertEquals(blocked(expectedReason), controller.state.value)
        assertNull(store.pass)
        assertEquals(listOf(WandKitAccessGateState.Passed(CACHED_PASS), blocked(expectedReason)), callbacks)
        assertEquals(listOf("dismiss", "present"), presenter.calls)
    }

    @Test
    fun cachedPass_networkError_staysPassed() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.failure(RuntimeException("offline")) }
        val controller = controller()

        controller.start(options)

        assertEquals(WandKitAccessGateState.Passed(CACHED_PASS), controller.state.value)
        assertEquals(CACHED_PASS, store.pass)
        assertEquals(1, callbacks.size)
    }

    @Test
    fun cachedPass_badApiKey_staysPassed() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.failure(AccessGateApiException(401, null)) }
        val controller = controller()

        controller.start(options)

        assertEquals(WandKitAccessGateState.Passed(CACHED_PASS), controller.state.value)
    }

    // Launch table: no cached pass

    @Test
    fun noPass_lastKnownDisabled_opensAtOnce_thenBlocksWhenNowEnabled() = runTest {
        store.lastKnownEnabled = false
        val response = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { response.await() }
        val controller = controller()

        controller.start(options)
        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertTrue(presenter.calls.isEmpty())

        response.complete(Result.success(status(enabled = true)))
        runCurrent()

        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), controller.state.value)
        assertEquals(true, store.lastKnownEnabled)
        assertEquals(listOf<WandKitAccessGateState>(blocked(WandKitAccessGateBlockReason.NoCode)), callbacks)
        assertEquals(listOf("present"), presenter.calls)
    }

    @Test
    fun noPass_lastKnownDisabled_staysDisabled_whenStillDisabled() = runTest {
        store.lastKnownEnabled = false
        repository.onStatus = { Result.success(status(enabled = false)) }
        val controller = controller()

        controller.start(options)

        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertEquals(listOf<String?>(null), repository.statusCalls)
        assertTrue(callbacks.isEmpty())
    }

    @Test
    fun noPass_lastKnownDisabled_networkError_staysDisabled() = runTest {
        store.lastKnownEnabled = false
        repository.onStatus = { Result.failure(RuntimeException("offline")) }
        val controller = controller()

        controller.start(options)

        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertTrue(presenter.calls.isEmpty())
    }

    @Test
    fun noPass_unknown_showsChecking_thenOpensWhenDisabled() = runTest {
        val response = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { response.await() }
        val controller = controller()

        controller.start(options)
        assertEquals(WandKitAccessGateState.Checking, controller.state.value)

        response.complete(Result.success(status(enabled = false)))
        runCurrent()

        assertEquals(WandKitAccessGateState.Disabled, controller.state.value)
        assertEquals(false, store.lastKnownEnabled)
        assertEquals(listOf(WandKitAccessGateState.Checking, WandKitAccessGateState.Disabled), callbacks)
        assertEquals(listOf("present", "dismiss"), presenter.calls)
    }

    @Test
    fun noPass_lastKnownEnabled_showsChecking_thenBlocksWithNoCode() = runTest {
        store.lastKnownEnabled = true
        repository.onStatus = {
            Result.success(status(enabled = true, title = "Beta", message = "Invite only beta", helpUrl = "https://example.com"))
        }
        val controller = controller()

        controller.start(options)

        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), controller.state.value)
        assertEquals(listOf(WandKitAccessGateState.Checking, blocked(WandKitAccessGateBlockReason.NoCode)), callbacks)
        assertEquals("Beta", controller.screen.value.serverTitle)
        assertEquals("Invite only beta", controller.screen.value.serverMessage)
        assertEquals("https://example.com", controller.screen.value.helpUrl)
    }

    @Test
    fun noPass_unknown_networkError_blocksOffline() = runTest {
        repository.onStatus = { Result.failure(RuntimeException("offline")) }
        val controller = controller()

        controller.start(options)

        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)
        assertNull(store.lastKnownEnabled)
    }

    @Test
    fun noPass_badApiKey_failsClosedAsOffline() = runTest {
        repository.onStatus = { Result.failure(AccessGateApiException(403, "wrong_api_key_kind")) }
        val controller = controller()

        controller.start(options)

        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)
    }

    // Timeouts

    @Test
    fun statusTimeout_withoutPass_blocksOffline() = runTest {
        repository.onStatus = { awaitCancellation() }
        val controller = controller()

        controller.start(options)
        advanceTimeBy(ACCESS_GATE_STATUS_TIMEOUT - 1.seconds)
        assertEquals(WandKitAccessGateState.Checking, controller.state.value)

        advanceTimeBy(2.seconds)
        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)
    }

    @Test
    fun statusTimeout_withPass_staysPassed() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { awaitCancellation() }
        val controller = controller()

        controller.start(options)
        advanceTimeBy(ACCESS_GATE_STATUS_TIMEOUT + 1.seconds)

        assertEquals(WandKitAccessGateState.Passed(CACHED_PASS), controller.state.value)
        assertEquals(CACHED_PASS, store.pass)
        assertEquals(1, callbacks.size)
    }

    @Test
    fun statusTimeout_lateResponseIsIgnored() = runTest {
        val response = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { response.await() }
        val controller = controller()

        controller.start(options)
        advanceTimeBy(ACCESS_GATE_STATUS_TIMEOUT + 1.seconds)
        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)

        response.complete(Result.success(status(enabled = false)))
        runCurrent()

        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)
        assertNull(store.lastKnownEnabled)
    }

    // Claim

    private fun TestScope.blockedController(): AccessGateController {
        store.lastKnownEnabled = true
        val controller = controller()
        controller.start(options)
        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), controller.state.value)
        callbacks.clear()
        presenter.calls.clear()
        return controller
    }

    @Test
    fun claim_success_storesThePass_andPasses() = runTest {
        val controller = blockedController()
        repository.onClaim = { Result.success(AccessGateClaim(code = "K7QM-2XFT", claimCount = 4)) }

        controller.submitCode("  k7qm 2xft ")

        val expected = WandKitAccessPass(code = "K7QM-2XFT", claimCount = 4, claimedAt = clock.now)
        assertEquals(listOf("k7qm 2xft"), repository.claimCalls)
        assertEquals(WandKitAccessGateState.Passed(expected), controller.state.value)
        assertEquals(expected, store.pass)
        assertEquals(true, store.lastKnownEnabled)
        assertFalse(controller.screen.value.isSubmitting)
        assertEquals(listOf<WandKitAccessGateState>(WandKitAccessGateState.Passed(expected)), callbacks)
        assertEquals(listOf("dismiss"), presenter.calls)
    }

    @Test
    fun claim_notFound_showsInlineError_andStaysOnTheField() = runTest {
        assertClaimShowsError(AccessGateApiException(404, ERROR_CODE_NOT_FOUND), AccessGateStrings.ERROR_NOT_FOUND)
    }

    @Test
    fun claim_expired_showsInlineError() = runTest {
        assertClaimShowsError(AccessGateApiException(409, ERROR_CODE_EXPIRED), AccessGateStrings.ERROR_EXPIRED)
    }

    @Test
    fun claim_revoked_showsInlineError() = runTest {
        assertClaimShowsError(AccessGateApiException(409, ERROR_CODE_REVOKED), AccessGateStrings.ERROR_REVOKED)
    }

    @Test
    fun claim_rateLimited_showsTryAgainInAMinute() = runTest {
        assertClaimShowsError(AccessGateApiException(429, null), AccessGateStrings.ERROR_RATE_LIMITED)
    }

    @Test
    fun claim_unexpectedApiError_showsGenericError() = runTest {
        assertClaimShowsError(AccessGateApiException(500, null), AccessGateStrings.ERROR_GENERIC)
    }

    private fun TestScope.assertClaimShowsError(error: AccessGateApiException, expectedMessage: String) {
        val controller = blockedController()
        repository.onClaim = { Result.failure(error) }

        controller.submitCode("K7QM-2XFT")

        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), controller.state.value)
        assertEquals(expectedMessage, controller.screen.value.claimErrorMessage)
        assertFalse(controller.screen.value.isSubmitting)
        assertNull(store.pass)
        assertTrue(callbacks.isEmpty())
    }

    @Test
    fun claim_transportError_showsOffline() = runTest {
        val controller = blockedController()
        repository.onClaim = { Result.failure(RuntimeException("offline")) }

        controller.submitCode("K7QM-2XFT")

        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)
        assertFalse(controller.screen.value.isSubmitting)
        assertEquals(listOf<WandKitAccessGateState>(blocked(WandKitAccessGateBlockReason.Offline)), callbacks)
    }

    @Test
    fun claim_timeout_showsOffline_andTheLateAnswerIsIgnored() = runTest {
        val controller = blockedController()
        val response = CompletableDeferred<Result<AccessGateClaim>>()
        repository.onClaim = { response.await() }

        controller.submitCode("K7QM-2XFT")
        advanceTimeBy(ACCESS_GATE_CLAIM_TIMEOUT - 1.seconds)
        assertTrue(controller.screen.value.isSubmitting)

        advanceTimeBy(2.seconds)
        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)
        assertFalse(controller.screen.value.isSubmitting)

        response.complete(Result.success(AccessGateClaim("K7QM-2XFT", 4)))
        runCurrent()
        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)
        assertNull(store.pass)
    }

    @Test
    fun claim_blankInput_neverCallsTheNetwork() = runTest {
        val controller = blockedController()

        controller.submitCode("")
        controller.submitCode("   \n\t")

        assertTrue(repository.claimCalls.isEmpty())
        assertFalse(controller.screen.value.isSubmitting)
    }

    @Test
    fun claim_whileAClaimIsInFlight_isIgnored() = runTest {
        val controller = blockedController()
        repository.onClaim = { awaitCancellation() }

        controller.submitCode("K7QM-2XFT")
        controller.submitCode("K7QM-2XFT")

        assertEquals(1, repository.claimCalls.size)
    }

    @Test
    fun claim_notBlocked_isIgnored() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.success(status(code = AccessGateCodeStatus.ACTIVE)) }
        val controller = controller()
        controller.start(options)

        controller.submitCode("K7QM-2XFT")

        assertTrue(repository.claimCalls.isEmpty())
    }

    @Test
    fun claim_newAttempt_clearsThePreviousError() = runTest {
        val controller = blockedController()
        repository.onClaim = { Result.failure(AccessGateApiException(404, ERROR_CODE_NOT_FOUND)) }
        controller.submitCode("WRONG")
        assertEquals(AccessGateStrings.ERROR_NOT_FOUND, controller.screen.value.claimErrorMessage)

        repository.onClaim = { awaitCancellation() }
        controller.submitCode("K7QM-2XFT")

        assertNull(controller.screen.value.claimErrorMessage)
        assertTrue(controller.screen.value.isSubmitting)
    }

    // Retry

    @Test
    fun retry_afterOffline_rerunsTheLaunchSequence() = runTest {
        repository.onStatus = { Result.failure(RuntimeException("offline")) }
        val controller = controller()
        controller.start(options)
        assertEquals(blocked(WandKitAccessGateBlockReason.Offline), controller.state.value)

        repository.onStatus = { Result.success(status(enabled = true)) }
        controller.retry()

        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), controller.state.value)
        assertEquals(2, repository.statusCalls.size)
        assertEquals(
            listOf(
                WandKitAccessGateState.Checking,
                blocked(WandKitAccessGateBlockReason.Offline),
                WandKitAccessGateState.Checking,
                blocked(WandKitAccessGateBlockReason.NoCode),
            ),
            callbacks,
        )
    }

    @Test
    fun retry_tappedRepeatedly_startsOneCheck_withoutDuplicateCallbacks() = runTest {
        repository.onStatus = { Result.failure(RuntimeException("offline")) }
        val controller = controller()
        controller.start(options)
        callbacks.clear()

        val response = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { response.await() }
        repeat(5) { controller.retry() }

        assertEquals(2, repository.statusCalls.size)
        assertEquals(listOf<WandKitAccessGateState>(WandKitAccessGateState.Checking), callbacks)

        response.complete(Result.success(status(enabled = true)))
        runCurrent()
        assertEquals(
            listOf(WandKitAccessGateState.Checking, blocked(WandKitAccessGateBlockReason.NoCode)),
            callbacks,
        )
    }

    @Test
    fun retry_whenNotBlocked_isIgnored() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.success(status(code = AccessGateCodeStatus.ACTIVE)) }
        val controller = controller()
        controller.start(options)

        controller.retry()

        assertEquals(1, repository.statusCalls.size)
    }

    // Reset

    @Test
    fun reset_clearsThePass_andRerunsTheLaunchSequence() = runTest {
        store.pass = CACHED_PASS
        store.lastKnownEnabled = true
        repository.onStatus = { code ->
            Result.success(status(code = if (code != null) AccessGateCodeStatus.ACTIVE else null))
        }
        val controller = controller()
        controller.start(options)
        assertEquals(WandKitAccessGateState.Passed(CACHED_PASS), controller.state.value)

        controller.reset()

        assertNull(store.pass)
        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), controller.state.value)
        assertEquals(listOf(CACHED_PASS.code, null), repository.statusCalls)
        assertEquals(
            listOf(
                WandKitAccessGateState.Passed(CACHED_PASS),
                WandKitAccessGateState.Checking,
                blocked(WandKitAccessGateBlockReason.NoCode),
            ),
            callbacks,
        )
    }

    @Test
    fun reset_duringAClaim_dropsTheLateSuccess_andClearsTheSpinner() = runTest {
        val controller = blockedController()
        val claimResponse = CompletableDeferred<Result<AccessGateClaim>>()
        repository.onClaim = { claimResponse.await() }
        controller.submitCode("K7QM-2XFT")
        assertTrue(controller.screen.value.isSubmitting)

        val statusResponse = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { statusResponse.await() }
        controller.reset()
        assertFalse(controller.screen.value.isSubmitting)
        assertEquals(WandKitAccessGateState.Checking, controller.state.value)

        claimResponse.complete(Result.success(AccessGateClaim("K7QM-2XFT", 4)))
        runCurrent()

        assertNull(store.pass)
        assertEquals(WandKitAccessGateState.Checking, controller.state.value)
    }

    @Test
    fun reset_duringAStatusCheck_dropsTheLateAnswer() = runTest {
        store.pass = CACHED_PASS
        val first = CompletableDeferred<Result<AccessGateStatus>>()
        val second = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { code -> if (code != null) first.await() else second.await() }
        val controller = controller()
        controller.start(options)

        controller.reset()
        first.complete(Result.success(status(enabled = false)))
        runCurrent()

        assertEquals(WandKitAccessGateState.Checking, controller.state.value)
        assertNull(store.lastKnownEnabled)
    }

    @Test
    fun reset_withoutOptions_isANoOp() = runTest {
        store.pass = CACHED_PASS
        val controller = controller()
        controller.start(null)

        controller.reset()

        assertEquals(CACHED_PASS, store.pass)
        assertTrue(repository.statusCalls.isEmpty())
    }

    // Callbacks, restart, shutdown

    @Test
    fun onStateChange_firesOncePerTransition_inOrder() = runTest {
        val controller = controller()
        controller.start(options)
        controller.submitCode("K7QM-2XFT")

        assertEquals(3, callbacks.size)
        assertEquals(WandKitAccessGateState.Checking, callbacks[0])
        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), callbacks[1])
        assertTrue(callbacks[2] is WandKitAccessGateState.Passed)
        assertEquals(listOf("present", "present", "dismiss"), presenter.calls)
    }

    @Test
    fun startTwice_doesNotStackTwoLaunchSequences() = runTest {
        val first = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { first.await() }
        val controller = controller()
        controller.start(options)

        val second = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { second.await() }
        controller.start(options)

        first.complete(Result.success(status(enabled = false)))
        runCurrent()
        assertEquals(WandKitAccessGateState.Checking, controller.state.value)

        second.complete(Result.success(status(enabled = true)))
        runCurrent()
        assertEquals(blocked(WandKitAccessGateBlockReason.NoCode), controller.state.value)
        assertEquals(listOf(WandKitAccessGateState.Checking, blocked(WandKitAccessGateBlockReason.NoCode)), callbacks)
    }

    @Test
    fun shutdown_stopsEverything_andDismisses() = runTest {
        val response = CompletableDeferred<Result<AccessGateStatus>>()
        repository.onStatus = { response.await() }
        val controller = controller()
        controller.start(options)

        controller.shutdown()
        response.complete(Result.success(status(enabled = true)))
        runCurrent()
        controller.retry()
        controller.reset()

        assertEquals(WandKitAccessGateState.Checking, controller.state.value)
        assertEquals(listOf<WandKitAccessGateState>(WandKitAccessGateState.Checking), callbacks)
        assertEquals(listOf("present", "dismiss"), presenter.calls)
        assertEquals(1, repository.statusCalls.size)
    }

    // Foreground re-check

    @Test
    fun foreground_withCachedPass_isThrottledToOncePerFiveMinutes() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.success(status(code = AccessGateCodeStatus.ACTIVE)) }
        val controller = controller()
        controller.start(options)
        assertEquals(1, repository.statusCalls.size)

        // The launch check counts as the first.
        clock.now += 4.minutes
        controller.onAppForegrounded()
        assertEquals(1, repository.statusCalls.size)

        clock.now += 1.minutes
        controller.onAppForegrounded()
        controller.onAppForegrounded()
        assertEquals(2, repository.statusCalls.size)

        clock.now += 1.minutes
        controller.onAppForegrounded()
        assertEquals(2, repository.statusCalls.size)
    }

    @Test
    fun foreground_findingTheCodeRevoked_blocks() = runTest {
        store.pass = CACHED_PASS
        repository.onStatus = { Result.success(status(code = AccessGateCodeStatus.ACTIVE)) }
        val controller = controller()
        controller.start(options)

        repository.onStatus = { Result.success(status(code = AccessGateCodeStatus.REVOKED)) }
        clock.now += 6.minutes
        controller.onAppForegrounded()

        assertEquals(blocked(WandKitAccessGateBlockReason.Revoked), controller.state.value)
        assertNull(store.pass)
    }

    @Test
    fun foreground_withoutAPass_doesNothing() = runTest {
        repository.onStatus = { Result.failure(RuntimeException("offline")) }
        val controller = controller()
        controller.start(options)

        clock.now += 10.minutes
        controller.onAppForegrounded()

        assertEquals(1, repository.statusCalls.size)
    }
}
