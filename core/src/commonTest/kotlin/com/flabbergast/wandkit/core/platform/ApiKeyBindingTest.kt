package com.flabbergast.wandkit.core.platform

import com.flabbergast.wandkit.core.data.accessgate.KEY_ACCESS_GATE_LAST_KNOWN_ENABLED
import com.flabbergast.wandkit.core.data.accessgate.KEY_ACCESS_GATE_PASS
import com.flabbergast.wandkit.core.data.referrals.KEY_DETECTION_ATTEMPTED
import com.flabbergast.wandkit.core.domain.install.createInstallIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyBindingTest {
    private val store = InMemoryKeyValueStore()

    private fun seed() {
        store.putString(KEY_ACCESS_GATE_PASS, "pass")
        store.putString(KEY_ACCESS_GATE_LAST_KNOWN_ENABLED, "true")
        store.putBoolean(KEY_DETECTION_ATTEMPTED, true)
        store.putString("wandkit.installId", "install-1")
        store.putString("wandkit.deviceId", "device-1")
        store.putString("wandkit.deviceIdLinkedUser", "user-1")
    }

    @Test
    fun sha256HexMatchesTheKnownDigest() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc"))
    }

    @Test
    fun changingTheKeyClearsGateAndReferralStateButKeepsInstallAndDeviceIdsAndTheLink() {
        discardStateOfPreviousApiKey(store, "wk_a")
        seed()

        discardStateOfPreviousApiKey(store, "wk_b")

        assertNull(store.getString(KEY_ACCESS_GATE_PASS))
        assertNull(store.getString(KEY_ACCESS_GATE_LAST_KNOWN_ENABLED))
        assertFalse(store.getBoolean(KEY_DETECTION_ATTEMPTED))
        assertEquals("install-1", store.getString("wandkit.installId"))
        assertEquals("device-1", store.getString("wandkit.deviceId"))
        // Who the device id belongs to does not depend on the key either.
        assertEquals("user-1", store.getString("wandkit.deviceIdLinkedUser"))
    }

    @Test
    fun aKeyChangeDoesNotRotateTheDeviceId_onTheNextLogin() {
        discardStateOfPreviousApiKey(store, "wk_a")
        val identity = createInstallIdentity(store)
        identity.linkDeviceIdTo("user-1")
        val deviceId = identity.deviceId

        discardStateOfPreviousApiKey(store, "wk_b")
        val relaunched = createInstallIdentity(store)
        relaunched.linkDeviceIdTo("user-1")

        assertEquals(deviceId, relaunched.deviceId)
        assertEquals(identity.installId, relaunched.installId)
    }

    @Test
    fun reconfiguringWithTheSameKeyKeepsState() {
        discardStateOfPreviousApiKey(store, "wk_a")
        discardStateOfPreviousApiKey(store, "wk_b")
        seed()

        discardStateOfPreviousApiKey(store, "wk_b")

        assertEquals("pass", store.getString(KEY_ACCESS_GATE_PASS))
        assertTrue(store.getBoolean(KEY_DETECTION_ATTEMPTED))
    }

    @Test
    fun firstRunWithNoStoredHashKeepsExistingState() {
        seed()

        discardStateOfPreviousApiKey(store, "wk_a")

        assertEquals("pass", store.getString(KEY_ACCESS_GATE_PASS))
        assertTrue(store.getBoolean(KEY_DETECTION_ATTEMPTED))
    }
}
