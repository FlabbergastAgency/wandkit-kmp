package com.flabbergast.wandkit.core.di

import com.flabbergast.wandkit.core.config.WandKitConfig
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What every event (and invite-code claim) says about who is using the app.
 * Configured without a platform context, so the store is in memory on Android
 * and the standard user defaults on iOS; relaunch stability over one store is
 * covered by `InstallIdentityTest`.
 */
class SdkContainerIdentityTest {
    private lateinit var container: WandKitSdkContainer

    @BeforeTest
    fun configure() {
        WandKitSdkContainer.init(WandKitConfig(apiKey = "wk_identity_test", isDebugLoggingEnabled = false))
        container = WandKitSdkContainer.get()
    }

    @Test
    fun anAnonymousInstallSendsNoUserId_andThePersistedDeviceId() {
        val identity = container.identityInfo

        assertNull(identity.userId)
        assertNull(identity.displayName)
        assertEquals(container.installIdentity.deviceId, identity.deviceId)
        // Not a fresh id per read (or per event), as it used to be per launch.
        assertEquals(identity.deviceId, container.identityInfo.deviceId)
    }

    @Test
    fun anIdentifiedUserIsSentWithTheSameDeviceId() {
        val anonymousDeviceId = container.identityInfo.deviceId

        container.setUserId("user-1", "Jane")
        val identity = container.identityInfo

        assertEquals("user-1", identity.userId)
        assertEquals("Jane", identity.displayName)
        assertEquals(anonymousDeviceId, identity.deviceId)
    }

    @Test
    fun clearingTheUserGoesBackToAnonymous_withoutANewDeviceId() {
        val deviceId = container.identityInfo.deviceId
        container.setUserId("user-1", "Jane")

        container.setUserId(null)
        val identity = container.identityInfo

        assertNull(identity.userId)
        assertNull(identity.displayName)
        assertEquals(deviceId, identity.deviceId)
    }

    @Test
    fun aBlankUserIdCountsAsAnonymous() {
        container.setUserId("   ", "Jane")

        val identity = container.identityInfo

        assertNull(identity.userId)
        assertNull(identity.displayName)
    }
}
