package com.flabbergast.wandkit.core.platform

import com.flabbergast.wandkit.core.data.accessgate.KEY_ACCESS_GATE_LAST_KNOWN_ENABLED
import com.flabbergast.wandkit.core.data.accessgate.KEY_ACCESS_GATE_PASS
import com.flabbergast.wandkit.core.data.referrals.KEY_DETECTION
import com.flabbergast.wandkit.core.data.referrals.KEY_DETECTION_ATTEMPTED
import com.flabbergast.wandkit.core.data.referrals.KEY_DETECTION_FAILURE_COUNT

internal expect fun sha256Hex(value: String): String

private const val KEY_API_KEY_HASH = "wandkit.apiKeyHash"

/**
 * A gate pass and referral attribution belong to the environment the key
 * points at. A build that switches from a legacy key to an app-bound one, or
 * from a staging key to a production one, must not keep the previous
 * environment's pass or referral state, so it is dropped the first time the SDK
 * sees a different key. Only a hash of the key is stored, never the key.
 *
 * The install and device ids are left alone - they identify the install, not
 * the key. With no stored hash (the first launch after this shipped) the
 * current one is recorded and nothing is cleared, so existing installs are not
 * disturbed.
 */
internal fun discardStateOfPreviousApiKey(
    keyValueStore: KeyValueStore,
    apiKey: String,
) {
    val current = sha256Hex(apiKey)
    val stored = keyValueStore.getString(KEY_API_KEY_HASH)
    if (stored == current) return
    if (stored != null) {
        listOf(
            KEY_ACCESS_GATE_PASS,
            KEY_ACCESS_GATE_LAST_KNOWN_ENABLED,
            KEY_DETECTION_ATTEMPTED,
            KEY_DETECTION,
            KEY_DETECTION_FAILURE_COUNT,
        ).forEach(keyValueStore::remove)
    }
    keyValueStore.putString(KEY_API_KEY_HASH, current)
}
