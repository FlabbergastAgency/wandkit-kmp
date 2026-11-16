package com.flabbergast.wandkit.core.domain.install

import com.flabbergast.wandkit.core.platform.KeyValueStore
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
    val installId: String

    /**
     * The anonymous id events (and invite-code claims) carry as `device_id`.
     * The backend keys an install nobody has identified as `dev:<device_id>`,
     * and ties a claim to the user the app later identifies through it, so it
     * must be the same on every launch. Stored under its own key, first
     * seeded from [installId] (so an existing install starts with device id
     * == install id), and never cleared by identify, logout or an API-key
     * change.
     */
    val deviceId: String

    /**
     * When the SDK first ran here. Fingerprint matching uses it to tell a fresh
     * install apart from one that merely reopened the app.
     */
    val firstLaunchAt: Instant
}

internal fun createInstallIdentity(keyValueStore: KeyValueStore): InstallIdentity = InstallIdentityImpl(keyValueStore)

internal const val KEY_INSTALL_ID = "wandkit.installId"
internal const val KEY_DEVICE_ID = "wandkit.deviceId"
private const val KEY_FIRST_LAUNCH_AT = "wandkit.firstLaunchAt"

@OptIn(ExperimentalUuidApi::class)
private class InstallIdentityImpl(
    private val keyValueStore: KeyValueStore,
) : InstallIdentity {
    override val installId: String by lazy {
        keyValueStore.getOrPut(KEY_INSTALL_ID) { Uuid.generateV4().toString() }
    }

    override val deviceId: String by lazy {
        keyValueStore.getOrPut(KEY_DEVICE_ID) { installId }
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
