package com.flabbergast.wandkit.core.domain.install

import com.flabbergast.wandkit.core.platform.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

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

    @Test
    fun aFreshDeviceIdIsNotLinked() {
        assertNull(createInstallIdentity(InMemoryKeyValueStore()).deviceIdLinkedUserId)
    }

    @Test
    fun linkingRemembersTheUser_withoutChangingTheId() {
        val store = InMemoryKeyValueStore()
        val identity = createInstallIdentity(store)
        val deviceId = identity.deviceId

        identity.linkDeviceIdTo("user-x")

        assertEquals(deviceId, identity.deviceId)
        assertEquals("user-x", identity.deviceIdLinkedUserId)
        assertEquals("user-x", store.getString(KEY_DEVICE_ID_LINKED_USER))
    }

    @Test
    fun linkingTheSameUserAgainChangesNothing() {
        val identity = createInstallIdentity(InMemoryKeyValueStore())
        identity.linkDeviceIdTo("user-x")
        val deviceId = identity.deviceId

        identity.linkDeviceIdTo("user-x")

        assertEquals(deviceId, identity.deviceId)
        assertEquals("user-x", identity.deviceIdLinkedUserId)
    }

    @Test
    fun linkingAnotherUserRotatesFirst() {
        val identity = createInstallIdentity(InMemoryKeyValueStore())
        identity.linkDeviceIdTo("user-x")
        val deviceIdOfX = identity.deviceId

        identity.linkDeviceIdTo("user-y")

        assertNotEquals(deviceIdOfX, identity.deviceId)
        assertEquals("user-y", identity.deviceIdLinkedUserId)
    }

    @Test
    fun unlinkingALinkedIdRotatesIt_andForgetsTheLink() {
        val store = InMemoryKeyValueStore()
        val identity = createInstallIdentity(store)
        identity.linkDeviceIdTo("user-x")
        val deviceIdOfX = identity.deviceId

        identity.unlinkDeviceId()

        assertNotEquals(deviceIdOfX, identity.deviceId)
        assertNull(identity.deviceIdLinkedUserId)
        assertNull(store.getString(KEY_DEVICE_ID_LINKED_USER))
        assertEquals(identity.deviceId, store.getString(KEY_DEVICE_ID))
    }

    @Test
    fun unlinkingTwiceRotatesOnlyOnce() {
        val identity = createInstallIdentity(InMemoryKeyValueStore())
        identity.linkDeviceIdTo("user-x")
        identity.unlinkDeviceId()
        val afterFirstLogout = identity.deviceId

        identity.unlinkDeviceId()

        assertEquals(afterFirstLogout, identity.deviceId)
    }

    @Test
    fun unlinkingWithNothingLinkedChangesNothing() {
        val store = InMemoryKeyValueStore()
        val deviceId = createInstallIdentity(store).deviceId

        // An app that clears the user on every signed-out launch.
        repeat(3) { createInstallIdentity(store).unlinkDeviceId() }

        assertEquals(deviceId, createInstallIdentity(store).deviceId)
    }

    @Test
    fun aRotatedIdSurvivesRelaunch() {
        val store = InMemoryKeyValueStore()
        val identity = createInstallIdentity(store)
        identity.linkDeviceIdTo("user-x")
        identity.unlinkDeviceId()

        assertEquals(identity.deviceId, createInstallIdentity(store).deviceId)
    }

    @Test
    fun theLinkSurvivesRelaunch() {
        val store = InMemoryKeyValueStore()
        val first = createInstallIdentity(store)
        first.linkDeviceIdTo("user-x")
        val deviceIdOfX = first.deviceId

        // Relaunched: the user id is in memory only, so the app identifies X again.
        val relaunched = createInstallIdentity(store)
        assertEquals("user-x", relaunched.deviceIdLinkedUserId)
        relaunched.linkDeviceIdTo("user-x")
        assertEquals(deviceIdOfX, relaunched.deviceId)

        // And Y signing in on the next launch gets a fresh id.
        val again = createInstallIdentity(store)
        again.linkDeviceIdTo("user-y")
        assertNotEquals(deviceIdOfX, again.deviceId)
    }

    @Test
    fun aLogoutAfterRelaunchStillRotates() {
        val store = InMemoryKeyValueStore()
        val first = createInstallIdentity(store)
        first.linkDeviceIdTo("user-x")
        val deviceIdOfX = first.deviceId

        createInstallIdentity(store).unlinkDeviceId()

        assertNotEquals(deviceIdOfX, createInstallIdentity(store).deviceId)
    }

    @Test
    fun aRotationIsSeenByAnotherIdentityOverTheSameStore() {
        // A re-configure creates a new identity while the old one may still
        // be serving requests: neither may keep a stale id.
        val store = InMemoryKeyValueStore()
        val old = createInstallIdentity(store)
        val new = createInstallIdentity(store)
        old.linkDeviceIdTo("user-x")
        assertEquals(old.deviceId, new.deviceId)

        new.unlinkDeviceId()

        assertEquals(new.deviceId, old.deviceId)
        assertNull(old.deviceIdLinkedUserId)
    }

    @Test
    fun theInstallIdNeverChanges() {
        val store = InMemoryKeyValueStore()
        val identity = createInstallIdentity(store)
        val installId = identity.installId

        identity.linkDeviceIdTo("user-x")
        identity.linkDeviceIdTo("user-y")
        identity.unlinkDeviceId()
        identity.unlinkDeviceId()

        assertEquals(installId, identity.installId)
        assertEquals(installId, createInstallIdentity(store).installId)
        assertNotEquals(installId, identity.deviceId)
    }

    @Test
    fun rotatedIdsAreFreshEachTime() {
        val identity = createInstallIdentity(InMemoryKeyValueStore())
        val seen = mutableSetOf(identity.deviceId)

        repeat(5) { index ->
            identity.linkDeviceIdTo("first-$index")
            identity.linkDeviceIdTo("second-$index")
            seen += identity.deviceId
            identity.unlinkDeviceId()
            seen += identity.deviceId
        }

        // The seed, then one per account switch and one per logout - all distinct.
        assertEquals(11, seen.size)
    }
}
