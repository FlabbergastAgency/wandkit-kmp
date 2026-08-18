package com.flabbergast.wandkit.core.domain.referrals

internal interface ReferralsRepository {
    suspend fun invite(
        userId: String,
        campaign: String,
        properties: Map<String, String>,
    ): ReferralInfo?

    suspend fun getReferral(path: String): GetReferralResponse?

    /** Null when the campaign does not exist, or this inviter has no referral yet. */
    suspend fun getReferralProgress(
        userId: String,
        campaign: String,
    ): ReferralProgress?

    /**
     * Asks the backend which referral this install came from, binding nothing.
     */
    suspend fun detectReferral(): ReferralDetection?

    /** The referral detected for this install, if detection has ever succeeded. */
    val detectedReferral: ReferralDetection?

    /** Forgets the detected referral once the question it answers is settled. */
    fun clearDetectedReferral()

    /**
     * Runs detection once per install, doing nothing on later calls.
     */
    suspend fun detectReferralOnFirstLaunchIfNeeded()

    /**
     * Redeems the Play install referrer code outright, without asking the user.
     *
     * Predates [detectReferral] and binds the install immediately. Prefer
     * detection unless you specifically want the old auto-claim behaviour.
     */
    suspend fun matchReferral(): ReferralMatch?

    suspend fun redeemCode(code: String): ReferralMatch?
}
