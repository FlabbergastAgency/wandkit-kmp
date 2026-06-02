package com.flabbergast.wandkit.core.data.events

import com.flabbergast.wandkit.core.data.events.dto.EventRequestDto
import com.flabbergast.wandkit.core.data.events.dto.EventResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal interface EventsApi {
    suspend fun trackEvent(request: EventRequestDto): WandKitHttpResponse<EventResponseDto>
}

internal fun createEventsApi(
    httpClient: WandKitHttpClient,
    baseUrl: String,
    logger: Logger,
): WandKitApi<EventsApi> = WandKitApi(
    api = EventsApiImpl(client = httpClient.client, baseUrl = baseUrl),
    logger = logger
)

private class EventsApiImpl(
    private val client: HttpClient,
    private val baseUrl: String,
) : EventsApi {

    override suspend fun trackEvent(request: EventRequestDto): WandKitHttpResponse<EventResponseDto> {
        val response = client.post("$baseUrl/api/v1/sdk/events") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

}