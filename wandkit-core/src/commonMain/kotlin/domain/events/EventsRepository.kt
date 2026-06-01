package com.flabbergast.wandkit.core.domain.events

internal interface EventsRepository {

    suspend fun trackEvent(event: WandKitEvent)
}