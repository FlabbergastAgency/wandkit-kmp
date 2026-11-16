package com.flabbergast.wandkit.core.domain.install

import com.flabbergast.wandkit.core.platform.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class InstallIdentityTest {
    @Test
    fun installIdSurvivesRelaunch() {
        val store = InMemoryKeyValueStore()

        // Two identities over one store stands in for two launches of one install.
        val first = createInstallIdentity(store).installId
        val second = createInstallIdentity(store).installId

        assertEquals(first, second)
    }

    @Test
    fun installIdIsUniquePerInstall() {
        val first = createInstallIdentity(InMemoryKeyValueStore()).installId
        val second = createInstallIdentity(InMemoryKeyValueStore()).installId

        assertNotEquals(first, second)
    }

    @Test
    fun firstLaunchAtIsRecordedOnce() {
        val store = InMemoryKeyValueStore()

        val first = createInstallIdentity(store).firstLaunchAt
        val second = createInstallIdentity(store).firstLaunchAt

        assertEquals(first, second)
    }

    @Test
    fun deviceIdSurvivesRelaunch() {
        val store = InMemoryKeyValueStore()

        val first = createInstallIdentity(store).deviceId
        val second = createInstallIdentity(store).deviceId

        assertEquals(first, second)
    }

    @Test
    fun deviceIdOfAFreshInstallIsSeededFromTheInstallId() {
        val identity = createInstallIdentity(InMemoryKeyValueStore())

        assertEquals(identity.installId, identity.deviceId)
    }

    @Test
    fun anExistingInstallStartsWithItsInstallIdAsDeviceId() {
        // A store written by an SDK version that only persisted the install id.
        val store = InMemoryKeyValueStore()
        store.putString(KEY_INSTALL_ID, "install-from-an-older-sdk")

        val identity = createInstallIdentity(store)

        assertEquals("install-from-an-older-sdk", identity.deviceId)
        assertEquals("install-from-an-older-sdk", store.getString(KEY_DEVICE_ID))
    }

    @Test
    fun deviceIdIsStoredUnderItsOwnKey_andOutlivesTheInstallId() {
        val store = InMemoryKeyValueStore()
        val deviceId = createInstallIdentity(store).deviceId

        // Whatever happens to the install id later, the device id stays.
        store.remove(KEY_INSTALL_ID)
        val relaunched = createInstallIdentity(store)

        assertEquals(deviceId, relaunched.deviceId)
        assertNotEquals(deviceId, relaunched.installId)
    }

    @Test
    fun deviceIdIsUniquePerInstall() {
        val first = createInstallIdentity(InMemoryKeyValueStore()).deviceId
        val second = createInstallIdentity(InMemoryKeyValueStore()).deviceId

        assertNotEquals(first, second)
    }
}
