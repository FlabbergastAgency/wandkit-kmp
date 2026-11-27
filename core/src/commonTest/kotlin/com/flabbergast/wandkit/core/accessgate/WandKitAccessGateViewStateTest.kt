package com.flabbergast.wandkit.core.accessgate

import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateScreenState
import com.flabbergast.wandkit.core.localization.WandKitText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(InternalWandKitApi::class)
class WandKitAccessGateViewStateTest {
    private val pass = WandKitAccessPass("K7QM-2XFT", 1, Instant.fromEpochMilliseconds(0))

    @Test
    fun phaseFollowsTheState() {
        val screen = AccessGateScreenState()
        assertEquals(WandKitAccessGateViewState.Phase.Checking, accessGateViewState(WandKitAccessGateState.Checking, screen).phase)
        assertEquals(WandKitAccessGateViewState.Phase.Hidden, accessGateViewState(WandKitAccessGateState.Disabled, screen).phase)
        assertEquals(WandKitAccessGateViewState.Phase.Hidden, accessGateViewState(WandKitAccessGateState.Passed(pass), screen).phase)
        assertEquals(
            WandKitAccessGateViewState.Phase.Offline,
            accessGateViewState(WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.Offline), screen).phase,
        )
        WandKitAccessGateBlockReason.entries.filter { it != WandKitAccessGateBlockReason.Offline }.forEach { reason ->
            assertEquals(
                WandKitAccessGateViewState.Phase.CodeEntry,
                accessGateViewState(WandKitAccessGateState.Blocked(reason), screen).phase,
            )
        }
    }

    @Test
    fun defaultsApplyWithoutServerCopy() {
        val view = accessGateViewState(WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.NoCode), AccessGateScreenState())

        assertEquals(WandKitText.ACCESS_GATE_TITLE.string, view.title)
        assertEquals(WandKitText.ACCESS_GATE_MESSAGE.string, view.message)
        assertNull(view.errorMessage)
        assertNull(view.helpUrl)
    }

    @Test
    fun serverCopyOverridesTheDefaults() {
        val view = accessGateViewState(
            WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.NoCode),
            AccessGateScreenState(serverTitle = "Beta", serverMessage = "Friends only", helpUrl = "https://example.com"),
        )

        assertEquals("Beta", view.title)
        assertEquals("Friends only", view.message)
        assertEquals("https://example.com", view.helpUrl)
    }

    @Test
    fun aRevokedCachedCodeExplainsItself() {
        val view = accessGateViewState(WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.Revoked), AccessGateScreenState())

        assertEquals(WandKitText.ACCESS_GATE_ERROR_REVOKED.string, view.errorMessage)
    }

    @Test
    fun anAlreadyUsedClaimShowsItsMessageOnTheCodeField() {
        val view = accessGateViewState(
            WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.NoCode),
            AccessGateScreenState(claimErrorMessage = WandKitText.ACCESS_GATE_ERROR_ALREADY_USED.string),
        )

        assertEquals(WandKitAccessGateViewState.Phase.CodeEntry, view.phase)
        assertEquals(WandKitText.ACCESS_GATE_ERROR_ALREADY_USED.string, view.errorMessage)
    }

    @Test
    fun aClaimErrorWinsOverTheBlockReason() {
        val view = accessGateViewState(
            WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.Expired),
            AccessGateScreenState(claimErrorMessage = WandKitText.ACCESS_GATE_ERROR_RATE_LIMITED.string, isSubmitting = false),
        )

        assertEquals(WandKitText.ACCESS_GATE_ERROR_RATE_LIMITED.string, view.errorMessage)
    }
}
