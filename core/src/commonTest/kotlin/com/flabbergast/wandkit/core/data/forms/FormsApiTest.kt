package com.flabbergast.wandkit.core.data.forms

import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormAnswerDto
import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormDeviceDto
import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormResponseRequestDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The `POST .../response` endpoint used to answer `204 No Content` and now
 * answers `200` with a small JSON body (`{"post_public_id": ...}`) that the SDK
 * ignores. These tests pin down that both shapes still decode cleanly into
 * `WandKitHttpResponse<Unit>` - the 200 case is the regression this DTO change
 * could have introduced, the 204 case is what the SDK already relied on.
 */
class FormsApiTest {
    @Test
    fun submitFormResponseSucceedsOn200WithJsonBody() = runBlocking {
        val formsApi = createTestFormsApi { _ ->
            respond(
                content = """{"post_public_id":"a3f1c2e4-1111-4444-8888-abcdefabcdef"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val result = formsApi { submitFormResponse(impressionId = "impression-1", request = testRequest()) }

        assertTrue(result.isSuccess, "Expected success but got $result")
    }

    @Test
    fun submitFormResponseSucceedsOn204NoContent() = runBlocking {
        val formsApi = createTestFormsApi { _ ->
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        val result = formsApi { submitFormResponse(impressionId = "impression-1", request = testRequest()) }

        assertTrue(result.isSuccess, "Expected success but got $result")
    }

    private fun testRequest() = SubmitFormResponseRequestDto(
        answers = listOf(
            SubmitFormAnswerDto(
                pageId = "page-1",
                thumb = null,
                stars = null,
                selectedOptionIds = null,
                text = "Great app",
            )
        ),
        completedAt = "2026-08-20T00:00:00Z",
        device = SubmitFormDeviceDto(
            platform = "android",
            osVersion = "14",
            appVersion = "1.2.3",
            deviceModel = "Pixel 8",
            locale = "en-US",
        ),
    )

    private fun createTestFormsApi(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): WandKitApi<FormsApi> {
        val json = createJson()
        val engine = MockEngine { request -> handler(request) }
        val client = HttpClient(engine) {
            // Mirrors the DefaultRequest block installed by the real
            // createHttpClient() - without an outgoing Content-Type,
            // ContentNegotiation skips serializing the request body entirely.
            install(DefaultRequest) { contentType(ContentType.Application.Json) }
            install(ContentNegotiation) { json(json) }
        }

        return createFormsApi(
            httpClient = WandKitHttpClient(client),
            baseUrl = "https://example.test",
            logger = NoOpLogger,
        )
    }
}

private object NoOpLogger : Logger {
    override fun verbose(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun debug(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun info(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun warn(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun error(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun assert(tag: String?, message: String, throwable: Throwable?) = Unit
}
