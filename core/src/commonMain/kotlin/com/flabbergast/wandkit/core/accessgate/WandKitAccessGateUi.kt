package com.flabbergast.wandkit.core.accessgate

import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateScreenState
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateStrings
import com.flabbergast.wandkit.core.domain.accessgate.cachedPassInlineMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Everything the gate screen renders, derived from the controller. */
@InternalWandKitApi
public data class WandKitAccessGateViewState(
    val phase: Phase,
    /** Server title when set, otherwise the SDK default. */
    val title: String,
    /** Server message when set, otherwise the SDK default. */
    val message: String,
    /** "Need a code?" target; `null` hides the link. */
    val helpUrl: String?,
    /** Inline error above the code field. */
    val errorMessage: String?,
    val isSubmitting: Boolean,
) {
    public enum class Phase {
        /** Nothing to block: the gate screen should close (or never open). */
        Hidden,
        Checking,
        CodeEntry,
        Offline,
    }

    public companion object {
        public val HIDDEN: WandKitAccessGateViewState = WandKitAccessGateViewState(
            phase = Phase.Hidden,
            title = AccessGateStrings.TITLE,
            message = AccessGateStrings.MESSAGE,
            helpUrl = null,
            errorMessage = null,
            isSubmitting = false,
        )
    }
}

/**
 * The bridge between the gate controller in `core` and the gate Activity in
 * `ui-compose`. Follows whichever controller the latest `configure` created,
 * so a screen that outlives a re-configure keeps working.
 */
@InternalWandKitApi
@OptIn(ExperimentalCoroutinesApi::class)
public object WandKitAccessGateUi {
    public val viewState: Flow<WandKitAccessGateViewState> =
        WandKitSdkContainer.activeAccessGate
            .flatMapLatest { controller ->
                controller?.let { combine(it.state, it.screen, ::accessGateViewState) }
                    ?: flowOf(WandKitAccessGateViewState.HIDDEN)
            }
            .distinctUntilChanged()

    /** The current value of [viewState], for a synchronous check when the screen is created. */
    public val currentViewState: WandKitAccessGateViewState
        get() {
            val controller = WandKitSdkContainer.activeAccessGate.value ?: return WandKitAccessGateViewState.HIDDEN
            return accessGateViewState(controller.state.value, controller.screen.value)
        }

    /**
     * The accent the host configured through the SDK -
     * [com.flabbergast.wandkit.core.config.WandKitFeedbackTheme.primaryColor],
     * a CSS hex string - or `null`. Known from `configure` on, so the gate is
     * branded from its very first frame.
     */
    public val configuredAccentColor: String?
        get() = WandKitSdkContainer.currentOrNull?.config?.feedbackTheme?.primaryColor

    public fun submitCode(code: String) {
        WandKitSdkContainer.activeAccessGate.value?.submitCode(code)
    }

    public fun retry() {
        WandKitSdkContainer.activeAccessGate.value?.retry()
    }
}

@OptIn(InternalWandKitApi::class)
internal fun accessGateViewState(
    state: WandKitAccessGateState,
    screen: AccessGateScreenState,
): WandKitAccessGateViewState {
    val phase = when (state) {
        WandKitAccessGateState.Checking -> WandKitAccessGateViewState.Phase.Checking
        is WandKitAccessGateState.Blocked ->
            if (state.reason == WandKitAccessGateBlockReason.Offline) {
                WandKitAccessGateViewState.Phase.Offline
            } else {
                WandKitAccessGateViewState.Phase.CodeEntry
            }
        WandKitAccessGateState.Disabled, is WandKitAccessGateState.Passed -> WandKitAccessGateViewState.Phase.Hidden
    }
    return WandKitAccessGateViewState(
        phase = phase,
        title = screen.serverTitle ?: AccessGateStrings.TITLE,
        message = screen.serverMessage ?: AccessGateStrings.MESSAGE,
        helpUrl = screen.helpUrl,
        // A claim failure wins; before any attempt, a cached code that went
        // bad explains why the gate is up at all.
        errorMessage = screen.claimErrorMessage
            ?: (state as? WandKitAccessGateState.Blocked)?.reason?.cachedPassInlineMessage,
        isSubmitting = screen.isSubmitting,
    )
}
