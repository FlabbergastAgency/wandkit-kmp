package com.flabbergast.wandkit.core.data.forms

import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormResponseRequestDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal interface FormsApi {
    suspend fun submitFormResponse(
        impressionId: String,
        request: SubmitFormResponseRequestDto,
    ): WandKitHttpResponse<Unit>
    suspend fun dismissForm(impressionId: String): WandKitHttpResponse<Unit>
}

internal fun createFormsApi(
    httpClient: WandKitHttpClient,
    baseUrl: String,
    logger: Logger,
): WandKitApi<FormsApi> = WandKitApi(
    api = FormsApiImpl(httpClient.client, baseUrl),
    logger = logger
)

private class FormsApiImpl(
    private val client: HttpClient,
    private val baseUrl: String,
): FormsApi {
    override suspend fun submitFormResponse(
        impressionId: String,
        request: SubmitFormResponseRequestDto
    ): WandKitHttpResponse<Unit> {
        val response = client.post("$baseUrl/api/v1/sdk/forms/$impressionId/response") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun dismissForm(impressionId: String): WandKitHttpResponse<Unit> {
        val response = client.post("$baseUrl/api/v1/sdk/forms/$impressionId/dismiss")
        return WandKitHttpResponse(response)
    }

}