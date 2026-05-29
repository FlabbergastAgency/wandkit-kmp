package com.flabbergast.wandkit.core.domain.events

import kotlin.time.Instant

internal data class WandKitEvent(
    val name: String,
    val properties: Map<String, String>,
    val occurredAt: Instant?,
)
