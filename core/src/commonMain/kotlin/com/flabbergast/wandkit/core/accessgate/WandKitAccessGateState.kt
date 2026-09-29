package com.flabbergast.wandkit.core.accessgate

import kotlin.time.Instant

/**
 * The invite gate's current state, reported to
 * [com.flabbergast.wandkit.core.config.WandKitAccessGateOptions.onStateChange]
 * and readable at any time from [com.flabbergast.wandkit.core.WandKit.accessGateState].
 */
public sealed interface WandKitAccessGateState {
    /**
     * The launch-time check is still in flight and there is no cached pass to
     * open with, so the blocking gate screen is up showing a spinner.
     */
    public data object Checking : WandKitAccessGateState

    /**
     * Gating is off: the host never passed `accessGate` options, the project
     * setting is off, or gating could not run on this setup. The app is open.
     */
    public data object Disabled : WandKitAccessGateState

    /** The gate screen is up, asking for a code. */
    public data class Blocked(
        val reason: WandKitAccessGateBlockReason,
    ) : WandKitAccessGateState

    /** A valid code is on file for this install. The app is open. */
    public data class Passed(
        val pass: WandKitAccessPass,
    ) : WandKitAccessGateState
}

/** Why the gate is blocking. Drives the gate screen's copy. */
public enum class WandKitAccessGateBlockReason {
    /** No code has ever been entered on this install. */
    NoCode,

    /** A cached code was revoked since it was claimed. */
    Revoked,

    /** A cached code expired since it was claimed. */
    Expired,

    /** A cached code is no longer known to the server. */
    NotFound,

    /**
     * The launch-time check could not reach the server (or timed out) and
     * there is no cached pass to fall back on. The screen offers "Try again".
     */
    Offline,
}

/** A claimed invite code, good until the server says otherwise. */
public data class WandKitAccessPass(
    /** The normalized, display-formatted code, e.g. `K7QM-2XFT`. */
    val code: String,
    /**
     * How many times this code has been claimed in total, across every
     * install, as returned by the claim call. Advisory only - verify codes on
     * your own server. The silent status re-check never changes it.
     */
    val claimCount: Int,
    /** When this install claimed the code. */
    val claimedAt: Instant,
)

internal val WandKitAccessGateState.isBlocking: Boolean
    get() = this is WandKitAccessGateState.Checking || this is WandKitAccessGateState.Blocked
