package com.flabbergast.wandkit.core.di

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.data.events.mappers.toEventRequestUser
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.platform.InMemoryKeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What every event (and invite-code claim) says about who is using the app.
 * Each test gets its own in-memory store; configuring again over the same
 * store stands in for a relaunch (the user id is in memory only, the device
 * id and its link are persisted).
 */
class SdkContainerIdentityTest {
    private val store = InMemoryKeyValueStore()
    private lateinit var container: WandKitSdkContainer

    @BeforeTest
    fun configure() {
        container = relaunch()
    }

    private fun relaunch(): WandKitSdkContainer {
        WandKitSdkContainer.init(
            WandKitConfig(apiKey = "wk_identity_test", isDebugLoggingEnabled = false),
            keyValueStore = store,
        )
        return WandKitSdkContainer.get()
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
    fun identifyingKeepsTheAnonymousDeviceId_soTheBackendCanMergeIt() {
        val anonymousDeviceId = container.identityInfo.deviceId

        container.identify("user-1", "Jane")
        val identity = container.identityInfo

        assertEquals("user-1", identity.userId)
        assertEquals("Jane", identity.displayName)
        assertEquals(anonymousDeviceId, identity.deviceId)
        assertEquals("user-1", container.installIdentity.deviceIdLinkedUserId)
    }

    @Test
    fun identifyingTheSameUserAgainChangesNothing() {
        container.identify("user-1", "Jane")
        val deviceId = container.identityInfo.deviceId

        container.identify("user-1")
        val identity = container.identityInfo

        assertEquals(deviceId, identity.deviceId)
        assertEquals("user-1", identity.userId)
        // A re-identify without a name keeps the suggestion.
        assertEquals("Jane", identity.displayName)
    }

    @Test
    fun clearingALinkedUserGoesBackToAnonymous_onAFreshDeviceId() {
        container.identify("user-1", "Jane")
        val deviceIdOfUser1 = container.identityInfo.deviceId

        container.clearUser()
        val identity = container.identityInfo

        assertNull(identity.userId)
        assertNull(identity.displayName)
        assertNotEquals(deviceIdOfUser1, identity.deviceId)
        assertNull(container.installIdentity.deviceIdLinkedUserId)
    }

    @Test
    fun clearingTwiceRotatesOnlyOnce() {
        container.identify("user-1")
        container.clearUser()
        val afterLogout = container.identityInfo.deviceId

        container.clearUser()

        assertEquals(afterLogout, container.identityInfo.deviceId)
    }

    @Test
    fun clearingWithNobodyLinkedKeepsTheDeviceId() {
        val deviceId = container.identityInfo.deviceId

        // An app that calls clearUser() on every signed-out launch.
        repeat(3) { container.clearUser() }

        assertEquals(deviceId, container.identityInfo.deviceId)
    }

    @Test
    fun switchingUserRotatesTheDeviceId_andDropsThePreviousName() {
        container.identify("user-1", "Jane")
        val deviceIdOfUser1 = container.identityInfo.deviceId

        container.identify("user-2")
        val identity = container.identityInfo

        assertEquals("user-2", identity.userId)
        assertNull(identity.displayName)
        assertNotEquals(deviceIdOfUser1, identity.deviceId)
        assertEquals("user-2", container.installIdentity.deviceIdLinkedUserId)
    }

    @Test
    fun theLinkOutlivesARelaunch() {
        container.identify("user-1")
        val deviceIdOfUser1 = container.identityInfo.deviceId

        val relaunched = relaunch()
        // Anonymous until the app identifies again - on the id the backend
        // already resolves to user-1.
        assertNull(relaunched.identityInfo.userId)
        assertEquals(deviceIdOfUser1, relaunched.identityInfo.deviceId)

        relaunched.identify("user-1")
        assertEquals(deviceIdOfUser1, relaunched.identityInfo.deviceId)

        relaunched.identify("user-2")
        assertNotEquals(deviceIdOfUser1, relaunched.identityInfo.deviceId)
    }

    @Test
    fun aLogoutAfterARelaunchStillRotates() {
        container.identify("user-1")
        val deviceIdOfUser1 = container.identityInfo.deviceId

        val relaunched = relaunch()
        relaunched.clearUser()

        assertNotEquals(deviceIdOfUser1, relaunched.identityInfo.deviceId)
        assertNull(relaunched.installIdentity.deviceIdLinkedUserId)
    }

    @Test
    fun aBlankIdIsAnonymous_butKeepsTheDeviceIdAndTheLink() {
        container.identify("user-1", "Jane")
        val deviceIdOfUser1 = container.identityInfo.deviceId

        for (blank in listOf("", "   ")) {
            container.identify(blank, "Jane")
            val identity = container.identityInfo

            assertNull(identity.userId)
            assertNull(identity.displayName)
            assertNull(container.externalUserId)
            assertEquals(deviceIdOfUser1, identity.deviceId)
            assertEquals("user-1", container.installIdentity.deviceIdLinkedUserId)
        }
    }

    @Test
    fun aBlankIdOnEveryLaunch_neverRotates() {
        // An app calling identify(user?.id ?: "") before its user has loaded.
        container.identify("user-1")
        val deviceIdOfUser1 = container.identityInfo.deviceId

        repeat(3) {
            val relaunched = relaunch()
            relaunched.identify("")
            relaunched.identify("  ")
            assertEquals(deviceIdOfUser1, relaunched.identityInfo.deviceId)
            assertEquals("user-1", relaunched.installIdentity.deviceIdLinkedUserId)
        }

        // Then the user loads: the same user keeps the id, another rotates.
        val loaded = relaunch()
        loaded.identify("")
        loaded.identify("user-1")
        assertEquals(deviceIdOfUser1, loaded.identityInfo.deviceId)
        assertEquals("user-1", loaded.identityInfo.userId)

        loaded.identify("")
        loaded.identify("user-2")
        assertNotEquals(deviceIdOfUser1, loaded.identityInfo.deviceId)
        assertEquals("user-2", loaded.installIdentity.deviceIdLinkedUserId)
    }

    @Test
    fun aBlankIdWithNobodyLinkedChangesNothing() {
        val deviceId = container.identityInfo.deviceId

        container.identify("", "Jane")

        assertNull(container.identityInfo.userId)
        assertNull(container.installIdentity.deviceIdLinkedUserId)
        assertEquals(deviceId, container.identityInfo.deviceId)
    }

    @Test
    fun idsAreComparedTrimmed() {
        container.identify(" user-1 ", "Jane")
        val deviceId = container.identityInfo.deviceId
        assertEquals("user-1", container.installIdentity.deviceIdLinkedUserId)

        container.identify("user-1")
        assertEquals(deviceId, container.identityInfo.deviceId)
        // The same user, so the suggested name stays.
        assertEquals("Jane", container.identityInfo.displayName)

        val relaunched = relaunch()
        relaunched.identify("\tuser-1  ")
        assertEquals(deviceId, relaunched.identityInfo.deviceId)

        relaunched.identify(" user-2")
        assertNotEquals(deviceId, relaunched.identityInfo.deviceId)
        assertEquals("user-2", relaunched.installIdentity.deviceIdLinkedUserId)
    }

    @Test
    fun theInstallIdSurvivesEverything() {
        val installId = container.installIdentity.installId

        container.identify("user-1")
        container.identify("")
        container.identify("user-2")
        container.clearUser()
        val relaunched = relaunch()
        relaunched.identify("user-3")
        relaunched.clearUser()

        assertEquals(installId, relaunched.installIdentity.installId)
    }

    @Test
    fun eventsCarryTheCurrentDeviceId_afterALogoutAndAnAccountSwitch() {
        container.identify("user-1")
        val asUser1 = container.identityInfo.toEventRequestUser()

        container.clearUser()
        val afterLogout = container.identityInfo.toEventRequestUser()

        container.identify("user-2")
        container.identify("user-3")
        val asUser3 = container.identityInfo.toEventRequestUser()

        assertEquals("user-1", asUser1.externalUserId)
        assertNull(afterLogout.externalUserId)
        assertNotEquals(asUser1.deviceId, afterLogout.deviceId)
        assertEquals("user-3", asUser3.externalUserId)
        assertNotEquals(afterLogout.deviceId, asUser3.deviceId)
        assertEquals(container.installIdentity.deviceId, asUser3.deviceId)
    }

    @Test
    fun aRequestNeverPairsAUserWithAnotherUsersDeviceId() = runTest {
        // A wrong pair would link the id to the wrong user for good. Readers
        // race identify/clear on real threads; every device id must only ever
        // be seen with one user, and never anonymous again after that.
        val snapshots = withContext(Dispatchers.Default) {
            val reader = async {
                val seen = mutableListOf<IdentifyInfo>()
                repeat(20_000) { seen += container.identityInfo }
                seen
            }
            val writer = async {
                repeat(300) {
                    container.identify("user-a")
                    container.identify("user-b")
                    container.clearUser()
                }
            }
            writer.await()
            reader.await()
        }

        val usersByDeviceId = mutableMapOf<String, String>()
        for (snapshot in snapshots) {
            val owner = usersByDeviceId[snapshot.deviceId]
            if (snapshot.userId == null) {
                assertNull(owner, "device id ${snapshot.deviceId} went anonymous after being used by $owner")
            } else {
                assertTrue(
                    owner == null || owner == snapshot.userId,
                    "device id ${snapshot.deviceId} seen with $owner and ${snapshot.userId}",
                )
                usersByDeviceId[snapshot.deviceId] = snapshot.userId
            }
        }
    }
}
