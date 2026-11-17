package com.flabbergast.wandkit.core.domain.install

import com.flabbergast.wandkit.core.platform.KeyValueStore
import com.flabbergast.wandkit.core.platform.createReentrantLock
import com.flabbergast.wandkit.core.platform.withLock
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Who this install is, as far as referrals, the invite gate and anonymous
 * events are concerned.
 *
 * A referral or gate claim binds an install permanently, so these ids have to
 * survive relaunches or an inviter loses a referral they earned.
 */
internal interface InstallIdentity {
    /**
     * Generated once per install and never changed afterwards - not by
     * identify, logout or an API-key change. The invite-code cap and referral
     * attribution key on it.
     */
    val installId: String

    /**
     * The anonymous id events (and invite-code claims) carry as `device_id`:
     * who is using the app until (and while) the app identifies them.
     *
     * The backend keys a user nobody has identified as `dev:<device_id>`. The
     * first time it sees this id together with a user id, it merges that
     * anonymous user into the identified one for good: from then on the id
     * belongs to that user, and a request carrying only this id counts as
     * theirs. So the id is persisted (the same on every launch, first seeded
     * from [installId] so an existing install starts with device id == install
     * id), and replaced by a fresh one whenever the person changes - see
     * [linkDeviceIdTo] and [unlinkDeviceId].
     *
     * Read it for every request rather than keeping it: it changes on logout
     * and account switch.
     */
    val deviceId: String

    /**
     * The user id the current [deviceId] was linked to by [linkDeviceIdTo],
     * or `null` while it has not been linked to anyone. Persisted, so it
     * outlives the in-memory user id across relaunches.
     */
    val deviceIdLinkedUserId: String?

    /**
     * Records that [deviceId] now belongs to [userId] (the app identified
     * them). When it is already linked to a different user, a fresh device id
     * is generated first, so the new user does not inherit the previous one's
     * anonymous id; linked to [userId] already, nothing changes.
     */
    fun linkDeviceIdTo(userId: String)

    /**
     * The user logged out: when [deviceId] is linked, replace it with a fresh
     * one and forget the link, so whoever uses the app next starts as a new
     * anonymous user. With nothing linked, nothing changes - an app that does
     * this on every signed-out launch must not get a new id per launch.
     */
    fun unlinkDeviceId()

    /**
     * When the SDK first ran here. Fingerprint matching uses it to tell a fresh
     * install apart from one that merely reopened the app.
     */
    val firstLaunchAt: Instant
}

internal fun createInstallIdentity(keyValueStore: KeyValueStore): InstallIdentity = InstallIdentityImpl(keyValueStore)

internal const val KEY_INSTALL_ID = "wandkit.installId"
internal const val KEY_DEVICE_ID = "wandkit.deviceId"
internal const val KEY_DEVICE_ID_LINKED_USER = "wandkit.deviceIdLinkedUser"
private const val KEY_FIRST_LAUNCH_AT = "wandkit.firstLaunchAt"

/**
 * Guards the device id and its link together. Process-wide rather than per
 * identity: there is one store per process, and a re-`configure` creates a new
 * identity over it while requests of the previous one may still be running.
 */
private val deviceIdLock = createReentrantLock()

@OptIn(ExperimentalUuidApi::class)
private class InstallIdentityImpl(
    private val keyValueStore: KeyValueStore,
) : InstallIdentity {
    override val installId: String by lazy {
        keyValueStore.getOrPut(KEY_INSTALL_ID) { Uuid.generateV4().toString() }
    }

    // Read from the store every time (both platforms keep it in memory), so
    // a rotation is seen by every reader, including other identities over the
    // same store.
    override val deviceId: String
        get() = deviceIdLock.withLock { keyValueStore.getOrPut(KEY_DEVICE_ID) { installId } }

    override val deviceIdLinkedUserId: String?
        get() = deviceIdLock.withLock { keyValueStore.getString(KEY_DEVICE_ID_LINKED_USER) }

    override fun linkDeviceIdTo(userId: String) {
        deviceIdLock.withLock {
            val linked = keyValueStore.getString(KEY_DEVICE_ID_LINKED_USER)
            if (linked == userId) return
            if (linked != null) rotate()
            keyValueStore.putString(KEY_DEVICE_ID_LINKED_USER, userId)
        }
    }

    override fun unlinkDeviceId() {
        deviceIdLock.withLock {
            if (keyValueStore.getString(KEY_DEVICE_ID_LINKED_USER) == null) return
            // The new id first: if the process dies between the two writes,
            // a stale link on a fresh id at worst rotates once more, while a
            // cleared link on the old id would hand the next person's
            // anonymous activity to the previous user.
            rotate()
            keyValueStore.remove(KEY_DEVICE_ID_LINKED_USER)
        }
    }

    /** Never touches [installId]: the invite-code cap keys on it. */
    private fun rotate() {
        keyValueStore.putString(KEY_DEVICE_ID, Uuid.generateV4().toString())
    }

    override val firstLaunchAt: Instant by lazy {
        val stored = keyValueStore.getOrPut(KEY_FIRST_LAUNCH_AT) { Clock.System.now().toString() }

        // An unparsable value can only come from a corrupted store; treating now
        // as the first launch is a better answer than failing to detect at all.
        runCatching { Instant.parse(stored) }.getOrElse { Clock.System.now() }
    }
}

private inline fun KeyValueStore.getOrPut(
    key: String,
    defaultValue: () -> String,
): String =
    getString(key) ?: defaultValue().also { putString(key, it) }
