package com.flabbergast.wandkit.core.accessgate

import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateScreenState
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateStrings
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

        assertEquals("Invite only", view.title)
        assertEquals("Enter your invite code to continue.", view.message)
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

        assertEquals(AccessGateStrings.ERROR_REVOKED, view.errorMessage)
    }

    @Test
    fun aClaimErrorWinsOverTheBlockReason() {
        val view = accessGateViewState(
            WandKitAccessGateState.Blocked(WandKitAccessGateBlockReason.Expired),
            AccessGateScreenState(claimErrorMessage = AccessGateStrings.ERROR_RATE_LIMITED, isSubmitting = false),
        )

        assertEquals(AccessGateStrings.ERROR_RATE_LIMITED, view.errorMessage)
    }
}
