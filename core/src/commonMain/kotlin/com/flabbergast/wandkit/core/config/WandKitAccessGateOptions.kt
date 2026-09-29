package com.flabbergast.wandkit.core.config

import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateState

/**
 * Opts an app into invite gating. Pass it as [WandKitConfig.accessGate] and
 * the SDK shows a blocking screen at launch asking for an invite code whenever
 * the project has gating turned on in the dashboard.
 *
 * Without it (`null`, the default) the SDK never makes a gate network call and
 * [com.flabbergast.wandkit.core.WandKit.accessGateState] stays
 * [WandKitAccessGateState.Disabled] - gating is a local opt-in, so an app that
 * never asked for it can never be covered by a screen it did not expect.
 *
 * Android only: it needs the `ui-compose` module (the gate screen lives there)
 * and `WandKit.configure(config, context)` called from `Application.onCreate`.
 * The iOS targets of this library log a warning and never gate; use the native
 * WandKit iOS SDK there.
 *
 * Compared by reference: two options holding equivalent but distinct lambdas
 * are not `equal`.
 */
public data class WandKitAccessGateOptions(
    /**
     * Called on the main thread for every state transition, never for a repeat
     * of the same state. Use it to log the user out on
     * [WandKitAccessGateState.Blocked] and to read the claimed code on
     * [WandKitAccessGateState.Passed].
     */
    val onStateChange: ((WandKitAccessGateState) -> Unit)? = null,
)
