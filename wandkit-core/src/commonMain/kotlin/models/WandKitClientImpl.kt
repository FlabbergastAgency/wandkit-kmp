package com.flabbergast.wandkit.core.models

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import kotlin.time.Instant

internal class WandKitClientImpl(
    private val config: WandKitConfig,
) : WandKitClient {
    override fun event(
        name: String,
        properties: Map<String, String>,
        occurredAt: Instant?,
    ) {
        WandKitSdkContainer.get().eventsRepository.reportEvent(
            WandKitEvent(
                name = name,
                properties = properties,
                occurredAt = occurredAt,
            )
        )
    }
}