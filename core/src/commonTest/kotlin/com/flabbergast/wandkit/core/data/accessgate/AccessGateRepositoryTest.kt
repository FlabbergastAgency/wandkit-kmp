package com.flabbergast.wandkit.core.data.accessgate

import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.WandKitHttpException
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateApiException
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateClaim
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateRepository
import com.flabbergast.wandkit.core.domain.install.InstallIdentity
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import com.flabbergast.wandkit.core.domain.accessgate.ACCESS_GATE_CLAIM_TIMEOUT
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private fun testHttpClient(engine: MockEngine): WandKitHttpClient {
    val json = createJson()
    return WandKitHttpClient(
        HttpClient(engine) {
            install(DefaultRequest) { contentType(ContentType.Application.Json) }
            install(ContentNegotiation) { json(json) }
        },
    )
}

private fun repository(
    externalUserId: () -> String? = { null },
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): Pair<AccessGateRepository, MockEngine> {
    val engine = MockEngine { request -> handler(request) }
    val api = createAccessGateApi(testHttpClient(engine), baseUrl = "https://example.test", logger = NoOpLogger)
    val installIdentity = object : InstallIdentity {
        override val installId = "install-1"

        // Deliberately not the install id, so a test can tell which one went out.
        override val deviceId = "device-1"
        override val firstLaunchAt = Instant.fromEpochMilliseconds(0)
    }
    return createAccessGateRepository(
        api,
        installIdentity,
        externalUserId = externalUserId,
        platform = "android",
        sdkVersion = "android-0.1.8",
    ) to engine
}

private fun HttpRequestData.jsonBody(): JsonObject =
    Json.parseToJsonElement((body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject

private const val CLAIM_OK = """{"code":"K7QM-2XFT","claim_count":4,"expires_at":null}"""

class AccessGateRepositoryTest {
    @Test
    fun claimSuccess_postsTheCodeAsTyped() = runTest {
        val (repository, engine) = repository {
            respond("""{"code":"K7QM-2XFT","claim_count":4,"expires_at":null}""", HttpStatusCode.OK, jsonHeaders)
        }

        val result = repository.claim("k7qm 2xft")

        assertEquals(AccessGateClaim("K7QM-2XFT", 4), result.getOrThrow())
        val request = engine.requestHistory.single()
        assertEquals("https://example.test/api/v1/sdk/access-gate/claim", request.url.toString())
        val body = Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
        assertEquals("k7qm 2xft", body["code"]?.jsonPrimitive?.content)
        assertEquals("install-1", body["install_id"]?.jsonPrimitive?.content)
        assertEquals("android", body["platform"]?.jsonPrimitive?.content)
        assertEquals("android-0.1.8", body["sdk_version"]?.jsonPrimitive?.content)
    }

    @Test
    fun anonymousClaim_sendsThePersistedDeviceId_andNoUserId() = runTest {
        val (repository, engine) = repository { respond(CLAIM_OK, HttpStatusCode.OK, jsonHeaders) }

        repository.claim("K7QM-2XFT")

        val body = engine.requestHistory.single().jsonBody()
        assertEquals("device-1", body["device_id"]?.jsonPrimitive?.content)
        assertFalse("external_user_id" in body)
    }

    @Test
    fun identifiedClaim_sendsTheUserIdToo() = runTest {
        val (repository, engine) = repository(externalUserId = { "user-1" }) {
            respond(CLAIM_OK, HttpStatusCode.OK, jsonHeaders)
        }

        repository.claim("K7QM-2XFT")

        val body = engine.requestHistory.single().jsonBody()
        assertEquals("device-1", body["device_id"]?.jsonPrimitive?.content)
        assertEquals("user-1", body["external_user_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun claim_readsTheUserIdAtClaimTime_andTreatsBlankAsAnonymous() = runTest {
        var userId: String? = "  "
        val (repository, engine) = repository(externalUserId = { userId }) {
            respond(CLAIM_OK, HttpStatusCode.OK, jsonHeaders)
        }

        repository.claim("K7QM-2XFT")
        userId = "user-2"
        repository.claim("K7QM-2XFT")

        val (first, second) = engine.requestHistory.map { it.jsonBody() }
        assertFalse("external_user_id" in first)
        assertEquals("user-2", second["external_user_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun statusCheck_carriesNoDeviceOrUserId() = runTest {
        val (repository, engine) = repository(externalUserId = { "user-1" }) {
            respond("""{"enabled":true}""", HttpStatusCode.OK, jsonHeaders)
        }

        repository.status("K7QM-2XFT")

        assertEquals(setOf("install_id", "code"), engine.requestHistory.single().jsonBody().keys)
    }

    @Test
    fun claim_socketTimeoutMatchesTheClaimTimeout() = runTest {
        val (repository, engine) = repository {
            respond("""{"code":"K7QM-2XFT","claim_count":4,"expires_at":null}""", HttpStatusCode.OK, jsonHeaders)
        }

        repository.claim("K7QM-2XFT")

        // Not the engine's 10 s read timeout: the controller's 15 s bound applies.
        val timeout = engine.requestHistory.single().getCapabilityOrNull(HttpTimeoutCapability)
        assertEquals(ACCESS_GATE_CLAIM_TIMEOUT.inWholeMilliseconds, timeout?.socketTimeoutMillis)
    }

    @Test
    fun claimNotFound_carriesTheErrorCode() = runTest {
        val (repository, _) = repository {
            respond(
                """{"error":"Not Found","code":"access_code_not_found","message":"access code not found"}""",
                HttpStatusCode.NotFound,
                jsonHeaders,
            )
        }

        val error = repository.claim("NOPE").exceptionOrNull()

        assertIs<AccessGateApiException>(error)
        assertEquals(404, error.statusCode)
        assertEquals("access_code_not_found", error.errorCode)
    }

    @Test
    fun claimConflict_distinguishesExpiredRevokedAndAlreadyUsed() = runTest {
        for (code in listOf("access_code_expired", "access_code_revoked", "access_code_already_used")) {
            val (repository, _) = repository {
                respond("""{"error":"Conflict","code":"$code","message":"x"}""", HttpStatusCode.Conflict, jsonHeaders)
            }

            val error = assertIs<AccessGateApiException>(repository.claim("K7QM").exceptionOrNull())
            assertEquals(409, error.statusCode)
            assertEquals(code, error.errorCode)
        }
    }

    @Test
    fun rateLimitedWithAPlainTextBody_hasNoErrorCode() = runTest {
        val (repository, _) = repository {
            respond("Too Many Requests", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.ContentType, "text/plain"))
        }

        val error = assertIs<AccessGateApiException>(repository.claim("K7QM").exceptionOrNull())
        assertEquals(429, error.statusCode)
        assertNull(error.errorCode)
    }

    @Test
    fun transportError_isNotAnApiError() = runTest {
        val (repository, _) = repository { throw IOException("no route to host") }

        val error = repository.status(null).exceptionOrNull()

        assertIs<IOException>(error)
    }

    @Test
    fun statusWithoutAPass_omitsTheCode() = runTest {
        val (repository, engine) = repository {
            respond("""{"enabled":true,"title":null,"message":null,"help_url":null,"code":null}""", HttpStatusCode.OK, jsonHeaders)
        }

        val status = repository.status(null).getOrThrow()

        assertEquals(true, status.enabled)
        val body = (engine.requestHistory.single().body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
        assertEquals("""{"install_id":"install-1"}""", body)
    }

    @Test
    fun cancellation_propagates_insteadOfBecomingAFailure() = runTest {
        val (repository, _) = repository { awaitCancellation() }
        var result: Result<*>? = null

        val job = launch { result = repository.claim("K7QM") }
        runCurrent()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(result)
    }

    @Test
    fun plainInvoke_stillLeavesTheErrorBodyOut() = runTest {
        val engine = MockEngine {
            respond("""{"error":"Not Found","code":"access_code_not_found"}""", HttpStatusCode.NotFound, jsonHeaders)
        }
        val httpClient = testHttpClient(engine).client
        val api = WandKitApi(api = httpClient, logger = NoOpLogger)

        val error = api<AccessGateStatusResponseDto> {
            WandKitHttpResponse(post("https://example.test/api/v1/sdk/access-gate/status") { setBody(AccessGateStatusRequestDto("install-1")) })
        }.exceptionOrNull()

        val httpError = assertIs<WandKitHttpException>(error)
        assertEquals(404, httpError.statusCode)
        assertNull(httpError.errorBody)
    }
}
