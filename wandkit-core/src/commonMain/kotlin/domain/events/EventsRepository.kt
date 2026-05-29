package com.flabbergast.wandkit.core.domain.events

internal interface EventsRepository {

    fun reportEvent(event: WandKitEvent)
}