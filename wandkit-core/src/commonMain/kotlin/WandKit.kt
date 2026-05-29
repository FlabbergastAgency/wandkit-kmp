package com.flabbergast.wandkit.core

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.models.WandKitClient
import com.flabbergast.wandkit.core.models.WandKitClientImpl
import kotlin.time.Instant

public object WandKit {
    private var instance: WandKitClient? = null

    public fun configure(
        config: WandKitConfig,
    ) {
        WandKitSdkContainer.init()
        instance = WandKitClientImpl(config)
    }

    public fun event(
        name: String,
        properties: Map<String, String> = emptyMap(),
        occurredAt: Instant? = null,
    ) {
        instance?.event(
            name = name,
            properties = properties,
            occurredAt = occurredAt,
        )
    }
}
