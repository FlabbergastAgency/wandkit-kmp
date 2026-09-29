package com.flabbergast.wandkit.core.data.accessgate

import com.flabbergast.wandkit.core.accessgate.WandKitAccessPass
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.platform.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AccessGateStoreTest {
    private val keyValueStore = InMemoryKeyValueStore()
    private val pass = WandKitAccessPass(
        code = "K7QM-2XFT",
        claimCount = 4,
        claimedAt = Instant.parse("2026-09-29T10:15:30.123Z"),
    )

    private fun store() = createAccessGateStore(keyValueStore, createJson())

    @Test
    fun passSurvivesRelaunch() {
        store().pass = pass

        // A second store over the same key-value store stands in for the next launch.
        assertEquals(pass, store().pass)
    }

    @Test
    fun clearingThePassRemovesIt() {
        store().pass = pass
        store().pass = null

        assertNull(store().pass)
        assertNull(keyValueStore.getString(KEY_ACCESS_GATE_PASS))
    }

    @Test
    fun corruptPassReadsAsNoPass() {
        keyValueStore.putString(KEY_ACCESS_GATE_PASS, "{not json")

        assertNull(store().pass)
    }

    @Test
    fun lastKnownEnabledIsUnknownUntilWritten() {
        assertNull(store().lastKnownEnabled)
    }

    @Test
    fun lastKnownEnabledRoundTripsBothValues() {
        store().lastKnownEnabled = false
        assertEquals(false, store().lastKnownEnabled)

        store().lastKnownEnabled = true
        assertEquals(true, store().lastKnownEnabled)

        store().lastKnownEnabled = null
        assertNull(store().lastKnownEnabled)
    }

    @Test
    fun usesTheSharedKeyNames() {
        store().pass = pass
        store().lastKnownEnabled = false

        assertEquals("false", keyValueStore.getString("wandkit.accessGate.lastKnownEnabled"))
        assertEquals(true, keyValueStore.getString("wandkit.accessGate.pass")?.contains("K7QM-2XFT"))
    }
}
