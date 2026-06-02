package com.flabbergast.wandkit.core.data.events

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.events.dto.EventRequestDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestSdkDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestUserDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.domain.events.WandKitEvent

internal fun createEventsRepository(
    eventsApi: WandKitApi<EventsApi>,
    appConfiguration: AppConfiguration,
): EventsRepository =
    EventsRepositoryImpl(
        eventsApi = eventsApi,
        appConfiguration = appConfiguration,
    )

internal class EventsRepositoryImpl(
    private val eventsApi: WandKitApi<EventsApi>,
    private val appConfiguration: AppConfiguration,
) : EventsRepository {
    override suspend fun trackEvent(
        event: WandKitEvent,
        identifyInfo: IdentifyInfo,
    ) {
        println("[matko] EventsRepoImpl trackEvent")
        eventsApi {
            println("[matko] eventsApi trackEvent")
            trackEvent(
                EventRequestDto(
                    eventName = event.name,
                    user = identifyInfo.toEventRequestUser(),
                    properties = event.properties,
                    occurredAt = event.occurredAt.toString(),
                    sdk = appConfiguration.toEventRequestSdk(),
                )
            )
        }
    }

    private fun IdentifyInfo.toEventRequestUser() = EventRequestUserDto(
        externalUserId = userId,
        deviceId = deviceId,
    )

    private fun AppConfiguration.toEventRequestSdk() = EventRequestSdkDto(
        platform = platformName,
        version = libraryVersion,
    )
}