package com.flabbergast.wandkit.core.data.events

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.events.dto.EventRequestDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestSdkDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestUserDto
import com.flabbergast.wandkit.core.data.events.mappers.toEventRequestSdk
import com.flabbergast.wandkit.core.data.events.mappers.toEventRequestUser
import com.flabbergast.wandkit.core.data.events.mappers.toFeedbackForm
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger

internal fun createEventsRepository(
    eventsApi: WandKitApi<EventsApi>,
    appConfiguration: AppConfiguration,
    logger: Logger,
): EventsRepository =
    EventsRepositoryImpl(
        eventsApi = eventsApi,
        appConfiguration = appConfiguration,
        logger = logger,
    )

private const val LOGGER_TAG = "[Event]"

private class EventsRepositoryImpl(
    private val eventsApi: WandKitApi<EventsApi>,
    private val appConfiguration: AppConfiguration,
    private val logger: Logger,
) : EventsRepository {
    override suspend fun trackEvent(
        event: WandKitEvent,
        identifyInfo: IdentifyInfo,
    ): FeedbackForm? =
        eventsApi {
            trackEvent(
                EventRequestDto(
                    eventName = event.name,
                    user = identifyInfo.toEventRequestUser(),
                    properties = event.properties,
                    occurredAt = event.occurredAt.toString(),
                    sdk = appConfiguration.toEventRequestSdk(),
                )
            )
        }.map {
            it.data.form?.let(::toFeedbackForm)
        }.onSuccess {
            logger.debug(LOGGER_TAG, "Event \"${event.name}\" successfully sent.")
        }.onFailure {
            logger.warn(LOGGER_TAG, "There has been an error with sending event \"${event.name}\", $it")
        }.getOrNull()
}