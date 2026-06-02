package com.flabbergast.wandkit.core.models

import kotlin.time.Instant

internal interface WandKitClient {
    fun trackEvent(
        name: String,
        properties: Map<String, String>,
        occurredAt: Instant?,
    )
}