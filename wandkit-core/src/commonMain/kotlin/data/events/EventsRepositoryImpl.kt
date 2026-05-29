package com.flabbergast.wandkit.core.data.events

import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.events.WandKitEvent

internal class EventsRepositoryImpl : EventsRepository {
    override fun reportEvent(event: WandKitEvent) {
        println("[matko] reporting $event")
    }
}