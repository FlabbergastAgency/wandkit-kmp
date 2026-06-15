package com.flabbergast.wandkit.core.domain.referrals

internal interface ReferralsRepository {
    suspend fun invite(
        userId: String,
        campaign: String,
        properties: Map<String, String>,
    ): ReferralInfo?

    suspend fun getReferral(path: String): GetReferralResponse?

    suspend fun matchReferral(): ReferralMatch?

    suspend fun redeemCode(code: String): ReferralMatch?
}
