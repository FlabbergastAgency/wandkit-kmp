package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.domain.referrals.ReferralDetection
import com.flabbergast.wandkit.core.platform.KeyValueStore
import kotlinx.serialization.json.Json

/**
 * Remembers what was detected, and whether detection has run at all.
 *
 * Detection has to happen at first launch while the fingerprint is still fresh,
 * which is long before the app has anywhere to show the result - so the answer
 * has to outlive the launch that produced it.
 */
internal interface ReferralDetectionStore {
    /** True once detection reached a definitive answer on this install. */
    val detectionAttempted: Boolean

    val detection: ReferralDetection?

    /** How many transient failures detection has already retried through. */
    val detectionFailureCount: Int

    fun setDetection(detection: ReferralDetection)

    fun clearDetection()

    fun markDetectionAttempted()

    fun recordDetectionFailure()
}

internal fun createReferralDetectionStore(
    keyValueStore: KeyValueStore,
    json: Json,
): ReferralDetectionStore = ReferralDetectionStoreImpl(keyValueStore, json)

private const val KEY_DETECTION_ATTEMPTED = "wandkit.referral.detectionAttempted"
private const val KEY_DETECTION = "wandkit.referral.detection"
private const val KEY_DETECTION_FAILURE_COUNT = "wandkit.referral.detectionFailureCount"

private class ReferralDetectionStoreImpl(
    private val keyValueStore: KeyValueStore,
    private val json: Json,
) : ReferralDetectionStore {
    override val detectionAttempted: Boolean
        get() = keyValueStore.getBoolean(KEY_DETECTION_ATTEMPTED)

    override val detection: ReferralDetection?
        get() = keyValueStore.getString(KEY_DETECTION)?.let { stored ->
            runCatching { json.decodeFromString(ReferralDetection.serializer(), stored) }.getOrNull()
        }

    override fun setDetection(detection: ReferralDetection) {
        val encoded = runCatching {
            json.encodeToString(ReferralDetection.serializer(), detection)
        }.getOrNull() ?: return
        keyValueStore.putString(KEY_DETECTION, encoded)
    }

    override val detectionFailureCount: Int
        get() = keyValueStore.getString(KEY_DETECTION_FAILURE_COUNT)?.toIntOrNull() ?: 0

    override fun clearDetection() {
        keyValueStore.remove(KEY_DETECTION)
    }

    override fun markDetectionAttempted() {
        keyValueStore.putBoolean(KEY_DETECTION_ATTEMPTED, true)
    }

    override fun recordDetectionFailure() {
        keyValueStore.putString(KEY_DETECTION_FAILURE_COUNT, (detectionFailureCount + 1).toString())
    }
}
