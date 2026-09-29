package com.flabbergast.wandkit.core.domain.accessgate

import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateBlockReason
import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateState
import com.flabbergast.wandkit.core.accessgate.WandKitAccessPass
import com.flabbergast.wandkit.core.accessgate.isBlocking
import com.flabbergast.wandkit.core.config.WandKitAccessGateOptions
import com.flabbergast.wandkit.core.data.accessgate.AccessGateStore
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Shows and hides the gate screen. Called on the controller's (main) thread:
 * [present] on every transition into [WandKitAccessGateState.Checking] or
 * [WandKitAccessGateState.Blocked] - including between two blocked reasons, so
 * a second call while the screen is already up must be a no-op - and
 * [dismiss] on every transition out of them.
 */
internal interface AccessGatePresenter {
    fun present()
    fun dismiss()
}

/** What the gate screen shows besides [WandKitAccessGateState] itself. */
internal data class AccessGateScreenState(
    /** A claim is in flight - the submit button's loading state. */
    val isSubmitting: Boolean = false,
    /** The last claim's inline error; cleared on every new attempt. */
    val claimErrorMessage: String? = null,
    /** Dashboard copy from the last status response; `null` uses the SDK default. */
    val serverTitle: String? = null,
    val serverMessage: String? = null,
    /** "Need a code?" link target; `null` hides the link. */
    val helpUrl: String? = null,
)

internal val ACCESS_GATE_STATUS_TIMEOUT: Duration = 10.seconds
internal val ACCESS_GATE_CLAIM_TIMEOUT: Duration = 15.seconds
internal val ACCESS_GATE_FOREGROUND_RECHECK_INTERVAL: Duration = 5.minutes

private const val LOGGER_TAG = "[AccessGate]"

/**
 * Drives the invite gate: the launch-time check against a cached pass (or the
 * lack of one), presenting and dismissing the gate screen, claiming a typed
 * code, and the throttled foreground re-check. Platform-neutral - the Android
 * Activity and the lifecycle glue sit behind [AccessGatePresenter] - so the
 * whole launch table of `plans/invite-gating.md` section 9 is unit-testable
 * with virtual time. A port of the iOS SDK's `WandKitAccessGateFlow`.
 *
 * Threading: every mutation runs on [dispatcher] (the main thread in
 * production, so [WandKitAccessGateOptions.onStateChange] and the presenter
 * are always called there). The one exception is the synchronous head of
 * [start], which runs on the caller's thread so a cached pass is readable
 * from [state] the moment `configure` returns.
 */
internal class AccessGateController(
    private val repository: AccessGateRepository,
    private val store: AccessGateStore,
    /** Resolved lazily in [start], so a host that never opted in never touches the platform UI. `null` = no gate screen available. */
    private val presenterFactory: () -> AccessGatePresenter?,
    /** `false` when the key-value store fell back to memory: a gate that forgets its pass every launch is unusable, so it stays off. */
    private val isStorePersistent: Boolean,
    private val logger: Logger,
    /**
     * For the errors that mean gating is off or cannot pass anyone because of
     * how the app is set up (no persistent storage, no gate screen, a rejected
     * API key). Unlike [logger] it must print even with debug logging off, or
     * a misconfigured release build would be silently ungated or stuck.
     * Never given the code or the API key.
     */
    private val misconfigurationLogger: Logger = logger,
    dispatcher: CoroutineDispatcher,
    private val clock: Clock = Clock.System,
    private val statusTimeout: Duration = ACCESS_GATE_STATUS_TIMEOUT,
    private val claimTimeout: Duration = ACCESS_GATE_CLAIM_TIMEOUT,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow<WandKitAccessGateState>(WandKitAccessGateState.Disabled)
    val state: StateFlow<WandKitAccessGateState> = _state

    private val _screen = MutableStateFlow(AccessGateScreenState())
    val screen: StateFlow<AccessGateScreenState> = _screen

    /** Other WandKit UI (feedback, feature preview, forms, screenshot card) must not show while this is true. */
    val isBlocking: Boolean
        get() = _state.value.isBlocking

    private var presenter: AccessGatePresenter? = null
    private var onStateChange: ((WandKitAccessGateState) -> Unit)? = null
    private var isConfigured = false
    private var statusJob: Job? = null
    private var claimJob: Job? = null
    private var foregroundRecheckJob: Job? = null

    /**
     * When a cached pass's status was last checked - by the launch sequence or
     * by [onAppForegrounded]. The launch check counts as the first, so a
     * foreground right after launch does not check again.
     */
    private var lastStatusCheckAt: Instant? = null

    /**
     * Called once from `configure`. `null` options mean the host never opted
     * in: no network call is ever made and [state] stays
     * [WandKitAccessGateState.Disabled]. Calling it again restarts the launch
     * sequence rather than stacking a second one.
     */
    fun start(options: WandKitAccessGateOptions?) {
        cancelWork()
        _screen.update { it.copy(isSubmitting = false, claimErrorMessage = null) }

        if (options == null) {
            onStateChange = null
            isConfigured = false
            return
        }

        if (!isStorePersistent) {
            misconfigurationLogger.error(
                LOGGER_TAG,
                "Invite gating is off: WandKit was configured without a Context, so it has no persistent storage " +
                    "and would ask for the code on every launch. Call WandKit.configure(config, applicationContext) " +
                    "from Application.onCreate.",
            )
            return
        }

        val presenter = presenterFactory()
        if (presenter == null) {
            misconfigurationLogger.error(LOGGER_TAG, "Invite gating is off: no gate screen is available on this setup (see the previous log line).")
            return
        }

        this.presenter = presenter
        onStateChange = options.onStateChange
        isConfigured = true
        runLaunchSequence()
    }

    /**
     * The offline screen's "Try again": re-runs the launch sequence. Only
     * while blocked - once it has moved on to checking, further taps are
     * no-ops, so mashing the button never starts parallel checks.
     */
    fun retry() = confined {
        if (!isConfigured || _state.value !is WandKitAccessGateState.Blocked) return@confined
        _screen.update { it.copy(claimErrorMessage = null) }
        runLaunchSequence()
    }

    /**
     * Clears the cached pass and re-runs the launch sequence as if this install
     * had never claimed a code (`WandKit.resetAccessGate()`, e.g. on logout).
     * In-flight checks and claims are cancelled; their late answers are dropped.
     */
    fun reset() = confined {
        if (!isConfigured) return@confined
        cancelWork()
        store.pass = null
        _screen.update { it.copy(isSubmitting = false, claimErrorMessage = null) }
        runLaunchSequence()
    }

    /** The gate screen's submit. Blank input never reaches the network. */
    fun submitCode(rawCode: String) = confined {
        if (_state.value !is WandKitAccessGateState.Blocked) return@confined
        val code = rawCode.trim()
        if (code.isEmpty() || _screen.value.isSubmitting) return@confined

        _screen.update { it.copy(isSubmitting = true, claimErrorMessage = null) }
        claimJob?.cancel()
        claimJob = scope.launch { claim(code) }
    }

    /**
     * The app came back to the foreground. With a cached pass, silently
     * re-checks it so a code revoked while backgrounded is noticed without a
     * cold start - at most once per [ACCESS_GATE_FOREGROUND_RECHECK_INTERVAL].
     * A no-op in every other state: checking/blocked already have their own
     * call or a "Try again" button.
     */
    fun onAppForegrounded() = confined {
        if (!isConfigured) return@confined
        val pass = (_state.value as? WandKitAccessGateState.Passed)?.pass ?: return@confined

        val now = clock.now()
        val last = lastStatusCheckAt
        if (last != null && now - last < ACCESS_GATE_FOREGROUND_RECHECK_INTERVAL) return@confined
        lastStatusCheckAt = now

        foregroundRecheckJob?.cancel()
        foregroundRecheckJob = scope.launch { recheckWithCachedPass(pass) }
    }

    /**
     * Tears this controller down for good - a later `configure` replaced it.
     * Nothing it had in flight can change state or call back any more.
     */
    fun shutdown() {
        isConfigured = false
        onStateChange = null
        scope.cancel()
        presenter?.dismiss()
    }

    // Launch sequence

    private fun runLaunchSequence() {
        statusJob?.cancel()

        val cachedPass = store.pass
        if (cachedPass != null) {
            setState(WandKitAccessGateState.Passed(cachedPass))
            lastStatusCheckAt = clock.now()
            statusJob = scope.launch { recheckWithCachedPass(cachedPass) }
            return
        }

        if (store.lastKnownEnabled != false) {
            // Enabled, or unknown (fail closed): cover the app while the check runs.
            setState(WandKitAccessGateState.Checking)
            statusJob = scope.launch { recheckWithoutCachedPass(wasChecking = true) }
            return
        }

        // No pass and gating was off last time: the app opens, and the
        // background check brings the gate up only if that changed.
        setState(WandKitAccessGateState.Disabled)
        statusJob = scope.launch { recheckWithoutCachedPass(wasChecking = false) }
    }

    /** A cached pass never blocks up front; only an answer from the server can take it away. */
    private suspend fun recheckWithCachedPass(cachedPass: WandKitAccessPass) {
        checkStatus(cachedPass.code)
            .onSuccess { status ->
                applyServerCopy(status)
                store.lastKnownEnabled = status.enabled

                if (!status.enabled) {
                    // Pass kept: a project that turns gating back on later
                    // should not have to ask this install again.
                    setState(WandKitAccessGateState.Disabled)
                    return
                }

                when (status.code?.status) {
                    AccessGateCodeStatus.ACTIVE -> Unit // Stay passed; only a claim changes the count.
                    AccessGateCodeStatus.REVOKED -> clearPassAndBlock(WandKitAccessGateBlockReason.Revoked)
                    AccessGateCodeStatus.EXPIRED -> clearPassAndBlock(WandKitAccessGateBlockReason.Expired)
                    AccessGateCodeStatus.NOT_FOUND -> clearPassAndBlock(WandKitAccessGateBlockReason.NotFound)
                    null -> {
                        // We sent a code; no verdict on it is unexpected. Fail
                        // closed rather than trust a pass the server ignored.
                        logger.warn(LOGGER_TAG, "Status check returned no code status for the cached pass; treating it as not found")
                        clearPassAndBlock(WandKitAccessGateBlockReason.NotFound)
                    }
                }
            }
            .onFailure { logStatusCheckFailure(it, "with a cached pass, staying passed") }
    }

    /**
     * No pass on file. [wasChecking] says whether the launch sequence put up
     * the checking cover (last known setting enabled or unknown) or left the
     * app open (last known setting disabled).
     */
    private suspend fun recheckWithoutCachedPass(wasChecking: Boolean) {
        checkStatus(code = null)
            .onSuccess { status ->
                applyServerCopy(status)
                store.lastKnownEnabled = status.enabled
                setState(
                    if (status.enabled) {
                        WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.NoCode)
                    } else {
                        WandKitAccessGateState.Disabled
                    },
                )
            }
            .onFailure { error ->
                if (wasChecking) {
                    logStatusCheckFailure(error, "with no cached pass, blocking offline")
                    setState(WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.Offline))
                } else {
                    // Gating was off last time and the app is already open:
                    // do not surprise it with a cover it cannot resolve.
                    logStatusCheckFailure(error, "with no cached pass while disabled, staying disabled")
                }
            }
    }

    /**
     * A timed-out check reads exactly like a transport error. Returns only
     * while still active: a check cancelled by [reset] or a newer launch
     * sequence throws here instead of touching state with a stale answer.
     */
    private suspend fun checkStatus(code: String?): Result<AccessGateStatus> {
        val result = withTimeoutOrNull(statusTimeout) { repository.status(code) }
            ?: Result.failure(AccessGateTimeoutException("status check"))
        currentCoroutineContext().ensureActive()
        return result
    }

    private fun clearPassAndBlock(reason: WandKitAccessGateBlockReason) {
        store.pass = null
        setState(WandKitAccessGateState.Blocked(reason))
    }

    private fun applyServerCopy(status: AccessGateStatus) {
        _screen.update {
            it.copy(serverTitle = status.title, serverMessage = status.message, helpUrl = status.helpUrl)
        }
    }

    /**
     * 401/403 (a bad or wrong-kind key) are logged apart from other failures
     * but fail the same way: from the outside a client cannot tell a broken
     * key from a probe, and opening the app would defeat the gate.
     */
    private fun logStatusCheckFailure(error: Throwable, context: String) {
        when {
            error is AccessGateApiException && (error.statusCode == 401 || error.statusCode == 403) ->
                misconfigurationLogger.error(
                    LOGGER_TAG,
                    "Status check rejected (HTTP ${error.statusCode}, ${error.errorCode ?: "no error code"}) - " +
                        "check that WandKitConfig.apiKey is this project's SDK key (wk_...), $context",
                )

            error is AccessGateApiException ->
                logger.error(LOGGER_TAG, "Status check failed (HTTP ${error.statusCode}, ${error.errorCode ?: "no error code"}), $context")

            error is AccessGateTimeoutException ->
                logger.warn(LOGGER_TAG, "Status check timed out after $statusTimeout, $context")

            else -> logger.warn(LOGGER_TAG, "Status check failed (transport error), $context", error)
        }
    }

    // Claim

    private suspend fun claim(code: String) {
        val result = withTimeoutOrNull(claimTimeout) { repository.claim(code) }
            ?: Result.failure(AccessGateTimeoutException("claim"))
        // A claim cancelled by reset (or restart) must not store a pass it
        // no longer asked for.
        currentCoroutineContext().ensureActive()

        _screen.update { it.copy(isSubmitting = false) }
        result
            .onSuccess { claim ->
                val pass = WandKitAccessPass(code = claim.code, claimCount = claim.claimCount, claimedAt = clock.now())
                store.pass = pass
                store.lastKnownEnabled = true
                logger.debug(LOGGER_TAG, "Invite code claimed (claim count ${claim.claimCount})")
                setState(WandKitAccessGateState.Passed(pass))
            }
            .onFailure(::handleClaimError)
    }

    private fun handleClaimError(error: Throwable) {
        if (error !is AccessGateApiException) {
            // Transport error or timeout: the code field is no help while
            // offline, so fold into the same offline screen the launch uses.
            logger.warn(LOGGER_TAG, "Claim failed (${if (error is AccessGateTimeoutException) "timed out" else "transport error"}), showing offline")
            setState(WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.Offline))
            return
        }

        val message = when {
            error.errorCode == ERROR_CODE_NOT_FOUND -> AccessGateStrings.ERROR_NOT_FOUND
            error.errorCode == ERROR_CODE_EXPIRED -> AccessGateStrings.ERROR_EXPIRED
            error.errorCode == ERROR_CODE_REVOKED -> AccessGateStrings.ERROR_REVOKED
            error.statusCode == 429 -> AccessGateStrings.ERROR_RATE_LIMITED
            else -> {
                logger.error(LOGGER_TAG, "Claim failed (HTTP ${error.statusCode}, ${error.errorCode ?: "no error code"})")
                AccessGateStrings.ERROR_GENERIC
            }
        }
        _screen.update { it.copy(claimErrorMessage = message) }
    }

    // State

    private fun setState(newState: WandKitAccessGateState) {
        if (_state.value == newState) return
        _state.value = newState

        // Captured now, dispatched in order: one presenter call and one
        // callback per real transition, always on the main thread.
        val presenter = presenter
        val callback = onStateChange
        scope.launch {
            if (newState.isBlocking) presenter?.present() else presenter?.dismiss()
            callback?.invoke(newState)
        }
    }

    private fun cancelWork() {
        statusJob?.cancel()
        claimJob?.cancel()
        foregroundRecheckJob?.cancel()
    }

    /** Runs [block] on the controller's thread - inline when already on it. */
    private inline fun confined(crossinline block: () -> Unit) {
        scope.launch { block() }
    }
}
