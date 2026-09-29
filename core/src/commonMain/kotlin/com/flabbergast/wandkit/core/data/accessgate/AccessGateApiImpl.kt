package com.flabbergast.wandkit.core.data.accessgate

import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimResponseDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.accessgate.ACCESS_GATE_CLAIM_TIMEOUT
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal fun createAccessGateApi(
    httpClient: WandKitHttpClient,
    baseUrl: String,
    logger: Logger,
): WandKitApi<AccessGateApi> = WandKitApi(
    api = AccessGateApiImpl(client = httpClient.client, baseUrl = baseUrl),
    logger = logger,
)

private class AccessGateApiImpl(
    private val client: HttpClient,
    private val baseUrl: String,
) : AccessGateApi {
    override suspend fun status(request: AccessGateStatusRequestDto): WandKitHttpResponse<AccessGateStatusResponseDto> {
        val response = client.post("$baseUrl/api/v1/sdk/access-gate/status") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun claim(request: AccessGateClaimRequestDto): WandKitHttpResponse<AccessGateClaimResponseDto> {
        val response = client.post("$baseUrl/api/v1/sdk/access-gate/claim") {
            setBody(request)
            // The engine's default 10 s read timeout would cut a slow claim
            // short of ACCESS_GATE_CLAIM_TIMEOUT (and show "offline" for a
            // claim the server may still record); the controller's bound is
            // the one that should apply.
            timeout { socketTimeoutMillis = ACCESS_GATE_CLAIM_TIMEOUT.inWholeMilliseconds }
        }
        return WandKitHttpResponse(response)
    }
}
