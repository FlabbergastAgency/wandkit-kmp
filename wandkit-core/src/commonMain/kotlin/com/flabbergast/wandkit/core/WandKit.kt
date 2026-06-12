package com.flabbergast.wandkit.core

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.models.WandKitClient
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
}
