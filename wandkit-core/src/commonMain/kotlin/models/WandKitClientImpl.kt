package com.flabbergast.wandkit.core.models

import com.flabbergast.wandkit.core.config.WandKitConfig
import kotlin.time.Instant

internal class WandKitClientImpl(
    private val config: WandKitConfig,
    private val presenter: WandKitPresenter,
) : WandKitClient {
    override fun event(
        name: String,
        properties: Map<String, String>,
        occurredAt: Instant?,
    ) {
        println("[WandKit] event: $name, properties: $properties, occurredAt: $occurredAt")
    }
}