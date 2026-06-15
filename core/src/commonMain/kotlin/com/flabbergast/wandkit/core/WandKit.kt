package com.flabbergast.wandkit.core

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.models.WandKitClient
import com.flabbergast.wandkit.core.domain.referrals.GetReferralResponse
import com.flabbergast.wandkit.core.domain.referrals.ReferralInfo
import com.flabbergast.wandkit.core.domain.referrals.ReferralMatch
import kotlin.time.Instant

public object WandKit {
    private val client: WandKitClient
        get() = WandKitSdkContainer.get().wandKitClient

    public fun configure(
        config: WandKitConfig,
    ) {
        WandKitSdkContainer.init(config)
    }

    public fun identify(
        userId: String,
    ) {
        WandKitSdkContainer.get().setUserId(userId)
    }

    public fun clearUser() {
        WandKitSdkContainer.get().setUserId(null)
    }

    public fun event(
        name: String,
        properties: Map<String, String> = emptyMap(),
        occurredAt: Instant? = null,
    ) {
        client.trackEvent(
            name = name,
            properties = properties,
            occurredAt = occurredAt,
        )
    }

    public suspend fun getInstallReferralCode(): String? = WandKitSdkContainer.get().installReferralCodeProvider.getReferralCode()

    public suspend fun invite(
        userId: String,
        campaign: String,
        properties: Map<String, String> = emptyMap(),
    ): ReferralInfo? = WandKitSdkContainer.get().referralsRepository.invite(userId, campaign, properties)

    public suspend fun getReferral(path: String): GetReferralResponse? =
        WandKitSdkContainer.get().referralsRepository.getReferral(path)

    public suspend fun matchReferral(): ReferralMatch? = WandKitSdkContainer.get().referralsRepository.matchReferral()

    public suspend fun redeemCode(code: String): ReferralMatch? =
        WandKitSdkContainer.get().referralsRepository.redeemCode(code)
}
