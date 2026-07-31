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
}
