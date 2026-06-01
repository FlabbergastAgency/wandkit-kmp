package com.flabbergast.wandkit.core.data.events

import com.flabbergast.wandkit.core.data.events.dto.EventRequestDto
import com.flabbergast.wandkit.core.data.events.dto.EventResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal interface EventsApi {
    suspend fun trackEvent(request: EventRequestDto): WandKitHttpResponse<EventResponseDto>
}

internal fun createEventsApi(
    httpClient: WandKitHttpClient,
    baseUrl: String,
): WandKitApi<EventsApi> = WandKitApi(
    EventsApiImpl(client = httpClient.client, baseUrl = baseUrl)
)

private const val PATH = "api/v1/sdk/events"

internal class EventsApiImpl(
    private val client: HttpClient,
    private val baseUrl: String,
) : EventsApi {

    override suspend fun trackEvent(request: EventRequestDto): WandKitHttpResponse<EventResponseDto> {
        val response = client.post("$baseUrl/$PATH") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

}