package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.referrals.ReferralDetection
import com.flabbergast.wandkit.core.platform.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReferralDetectionStoreTest {
    @Test
    fun detectionSurvivesRelaunch() {
        val store = InMemoryKeyValueStore()
        val json = createJson()
        val detection = ReferralDetection(
            referralId = "a3f1",
            code = "INVITE10",
            campaign = "invite-5-for-1-year",
            campaignName = "Invite 5",
            inviterId = "user_123",
            properties = mapOf("source" to "profile"),
        )

        createReferralDetectionStore(store, json).setDetection(detection)

        assertEquals(detection, createReferralDetectionStore(store, json).detection)
    }

    @Test
    fun detectionIsAbsentUntilOneIsStored() {
        assertNull(createReferralDetectionStore(InMemoryKeyValueStore(), createJson()).detection)
    }

    @Test
    fun clearedDetectionStaysCleared() {
        val store = InMemoryKeyValueStore()
        val json = createJson()
        val detection = ReferralDetection(
            referralId = "a3f1",
            code = "INVITE10",
            campaign = "invite-5-for-1-year",
            inviterId = "user_123",
        )

        createReferralDetectionStore(store, json).setDetection(detection)
        createReferralDetectionStore(store, json).clearDetection()

        assertNull(createReferralDetectionStore(store, json).detection)
    }

    @Test
    fun failureCountAccumulatesAcrossLaunches() {
        val store = InMemoryKeyValueStore()
        val json = createJson()

        assertEquals(0, createReferralDetectionStore(store, json).detectionFailureCount)
        createReferralDetectionStore(store, json).recordDetectionFailure()
        createReferralDetectionStore(store, json).recordDetectionFailure()

        assertEquals(2, createReferralDetectionStore(store, json).detectionFailureCount)
    }

    @Test
    fun detectionAttemptIsRecordedOnce() {
        val store = InMemoryKeyValueStore()
        val json = createJson()

        assertFalse(createReferralDetectionStore(store, json).detectionAttempted)
        createReferralDetectionStore(store, json).markDetectionAttempted()

        assertTrue(createReferralDetectionStore(store, json).detectionAttempted)
    }
}
