package com.flabbergast.wandkit.core.data.events

import com.flabbergast.wandkit.core.data.events.dto.EventRequestDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestSdkDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestUserDto
import com.flabbergast.wandkit.core.data.events.dto.EventResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import com.flabbergast.wandkit.core.domain.threading.BackgroundDispatcher

internal fun createEventsRepository(eventsApi: WandKitApi<EventsApi>): EventsRepository =
    EventsRepositoryImpl(eventsApi)

internal class EventsRepositoryImpl(
    private val eventsApi: WandKitApi<EventsApi>,
) : EventsRepository {
    override suspend fun trackEvent(event: WandKitEvent) {
        eventsApi {
            trackEvent(
                EventRequestDto(
                    eventName = event.name,
                    user = EventRequestUserDto(
                        externalUserId = "aaa",
                        deviceId = "aaa",
                    ),
                    properties = event.properties,
                    occurredAt = event.occurredAt?.toString() ?: "",
                    sdk = EventRequestSdkDto(
                        platform = "Android",
                        version = "1.0",
                    )
                )
            )
        }
    }
}