package com.flabbergast.wandkit.core.models

import kotlin.time.Instant

internal interface WandKitClient {
    fun event(
        name: String,
        properties: Map<String, String>,
        occurredAt: Instant?,
    )
}