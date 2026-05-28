package com.flabbergast.wandkit.core

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.models.WandKitClient
import com.flabbergast.wandkit.core.models.WandKitClientImpl
import com.flabbergast.wandkit.core.models.WandKitPresenter
import kotlin.time.Instant

public object WandKit {
    private var instance: WandKitClient? = null

    public fun configure(
        config: WandKitConfig,
        presenter: WandKitPresenter,
    ) {
        instance = WandKitClientImpl(config, presenter)
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
