package com.flabbergast.wandkit.core.data.posts

import com.flabbergast.wandkit.core.config.createAppConfiguration
import com.flabbergast.wandkit.core.data.networking.WandKitHttpException
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreateAttachmentRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreatePostRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionRequestDto
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import com.flabbergast.wandkit.core.testutil.createTestPostsApi
import com.flabbergast.wandkit.core.testutil.createTestPostsApiWithInterceptor
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostsApiTest {
    @Test
    fun createSessionDecodesSuccessBody() = runBlocking {
        val postsApi = createTestPostsApi { _ ->
            respond(
                content = """
                    {"token":"tok","expires_at":"2026-08-21T10:00:00Z","read_only":false,
                    "config":{"enabled_types":["bug","feature_request"],"roadmap_enabled":true,
                    "tags":[{"id":"t1","name":"iOS","color":"#FF0000"}]}}
                """.trimIndent().replace("\n", ""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val result = postsApi { createSession(testSessionRequest()) }

        val data = result.getOrThrow().data
        assertEquals("tok", data.token)
        assertEquals("2026-08-21T10:00:00Z", data.expiresAt)
        assertFalse(data.readOnly)
        assertEquals(listOf("bug", "feature_request"), data.config.enabledTypes)
        assertTrue(data.config.roadmapEnabled)
        assertEquals(1, data.config.tags.size)
        assertEquals("t1", data.config.tags[0].id)
        assertEquals("iOS", data.config.tags[0].name)
        assertEquals("#FF0000", data.config.tags[0].color)
    }

    @Test
    fun createSessionFailsOn403() = runBlocking {
        val postsApi = createTestPostsApi { _ ->
            respond(content = "", status = HttpStatusCode.Forbidden)
        }

        val result = postsApi { createSession(testSessionRequest()) }

        assertTrue(result.isFailure, "Expected failure but got $result")
        val exception = result.exceptionOrNull()
        assertTrue(exception is WandKitHttpException, "Expected WandKitHttpException but got $exception")
        assertEquals(403, exception.statusCode)
    }

    @Test
    fun createSessionBodyOmitsAbsentAttributes() = runBlocking {
        var capturedBody: String? = null
        val postsApi = createTestPostsApi { request ->
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(
                content = """{"token":"tok","expires_at":"2026-08-21T10:00:00Z","config":{}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        postsApi { createSession(testSessionRequest()) }

        val body = requireNotNull(capturedBody)
        assertTrue(body.contains(""""external_user_id":"u1""""), "Expected external_user_id in $body")
        assertTrue(body.contains(""""platform":"android""""), "Expected platform in $body")
        assertFalse(body.contains(""""attributes""""), "Did not expect attributes key in $body")
    }

    @Test
    fun createSessionBodyIncludesDisplayNameWhenSet() = runBlocking {
        var capturedBody: String? = null
        val postsApi = createTestPostsApi { request ->
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(
                content = """{"token":"tok","expires_at":"2026-08-21T10:00:00Z","config":{}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        postsApi { createSession(testSessionRequest(displayName = "Jane")) }

        val body = requireNotNull(capturedBody)
        assertTrue(body.contains(""""display_name":"Jane""""), "Expected display_name in $body")
    }

    @Test
    fun createSessionBodyOmitsDisplayNameWhenAbsent() = runBlocking {
        var capturedBody: String? = null
        val postsApi = createTestPostsApi { request ->
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(
                content = """{"token":"tok","expires_at":"2026-08-21T10:00:00Z","config":{}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        postsApi { createSession(testSessionRequest()) }

        val body = requireNotNull(capturedBody)
        assertFalse(body.contains(""""display_name""""), "Did not expect display_name key in $body")
    }

    @Test
    fun mintSessionMapsDisplayNameFromResponseToDomain() = runBlocking {
        val postsApi = createTestPostsApi { _ ->
            respond(
                content = """{"token":"tok","expires_at":"2026-08-21T10:00:00Z","display_name":"Jane","config":{}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val repository = createPostsSessionRepository(
            postsApi = postsApi,
            appConfiguration = createAppConfiguration(isDebugLoggingEnabled = false, apiBaseUrl = "https://example.test"),
            platformContext = null,
            externalUserId = { "u1" },
            displayName = { null },
            logger = NoOpLogger,
        )

        val session = repository.mintSession().getOrThrow()

        assertEquals("Jane", session.displayName)
    }

    @Test
    fun createAttachmentSendsBearerTokenAndSnakeCaseBody() = runBlocking {
        var capturedAuth: String? = null
        var capturedBody: String? = null
        val postsApi = createTestPostsApi { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(
                content = """{"id":"att-1","upload_url":"/api/v1/sdk/posts/attachments/att-1/upload"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val result = postsApi {
            createAttachment(
                "tok",
                SdkCreateAttachmentRequestDto(kind = "image", contentType = "image/jpeg", sizeBytes = 42),
            )
        }

        assertEquals("Bearer tok", capturedAuth)
        val body = requireNotNull(capturedBody)
        assertTrue(body.contains(""""kind":"image""""), "Expected kind in $body")
        assertTrue(body.contains(""""content_type":"image/jpeg""""), "Expected content_type in $body")
        assertTrue(body.contains(""""size_bytes":42"""), "Expected size_bytes in $body")

        val data = result.getOrThrow().data
        assertEquals("att-1", data.id)
        assertEquals("/api/v1/sdk/posts/attachments/att-1/upload", data.uploadUrl)
    }

    @Test
    fun createPostSendsBearerTokenAndSnakeCaseBody() = runBlocking {
        var capturedAuth: String? = null
        var capturedBody: String? = null
        val postsApi = createTestPostsApi { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(
                content = """{"id":"post-1"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val result = postsApi {
            createPost(
                "tok",
                SdkCreatePostRequestDto(text = "It crashes", type = "bug", attachmentIds = listOf("att-1")),
            )
        }

        assertEquals("Bearer tok", capturedAuth)
        val body = requireNotNull(capturedBody)
        assertTrue(body.contains(""""text":"It crashes""""), "Expected text in $body")
        assertTrue(body.contains(""""type":"bug""""), "Expected type in $body")
        assertTrue(body.contains(""""attachment_ids":["att-1"]"""), "Expected attachment_ids in $body")

        assertEquals("post-1", result.getOrThrow().data.id)
    }

    @Test
    fun uploadAttachmentToAnAbsoluteUrlSendsNoAuthorizationAndNoApiKey() = runBlocking {
        var capturedAuth: String? = null
        var capturedApiKey: String? = null
        var capturedContentType: ContentType? = null
        var capturedHost: String? = null
        val postsApi = createTestPostsApiWithInterceptor(apiHost = "example.test") { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            capturedApiKey = request.headers["X-API-Key"]
            capturedContentType = request.body.contentType
            capturedHost = request.url.host
            respond(content = "", status = HttpStatusCode.OK)
        }

        val result = postsApi.status {
            uploadAttachment(
                url = "https://cdn.foreign-host.test/upload/abc",
                bytes = byteArrayOf(1, 2, 3),
                contentType = "image/jpeg",
                bearer = "tok",
            )
        }

        assertTrue(result.isSuccess, "Expected success but got $result")
        assertEquals("cdn.foreign-host.test", capturedHost)
        assertNull(capturedAuth, "Did not expect an Authorization header on a foreign host")
        assertNull(capturedApiKey, "Did not expect the project API key on a foreign host")
        assertEquals(ContentType.parse("image/jpeg"), capturedContentType)
    }

    @Test
    fun uploadAttachmentToARelativeUrlResolvesAgainstBaseUrlAndSendsAuthAndApiKey() = runBlocking {
        var capturedAuth: String? = null
        var capturedApiKey: String? = null
        var capturedUrl: String? = null
        val postsApi = createTestPostsApiWithInterceptor(apiHost = "example.test") { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            capturedApiKey = request.headers["X-API-Key"]
            capturedUrl = request.url.toString()
            respond(content = "", status = HttpStatusCode.OK)
        }

        val result = postsApi.status {
            uploadAttachment(
                url = "/api/v1/sdk/posts/attachments/abc/upload",
                bytes = byteArrayOf(1, 2, 3),
                contentType = "image/png",
                bearer = "tok",
            )
        }

        assertTrue(result.isSuccess, "Expected success but got $result")
        assertEquals("https://example.test/api/v1/sdk/posts/attachments/abc/upload", capturedUrl)
        assertEquals("Bearer tok", capturedAuth)
        assertEquals("test-api-key", capturedApiKey)
    }

    @Test
    fun uploadAttachmentFailsOnNon2xxStatus() = runBlocking {
        val postsApi = createTestPostsApiWithInterceptor(apiHost = "example.test") { _ ->
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }

        val result = postsApi.status {
            uploadAttachment(
                url = "https://cdn.foreign-host.test/upload/abc",
                bytes = byteArrayOf(1, 2, 3),
                contentType = "image/jpeg",
                bearer = "tok",
            )
        }

        assertTrue(result.isFailure, "Expected failure but got $result")
        val exception = result.exceptionOrNull()
        assertTrue(exception is WandKitHttpException, "Expected WandKitHttpException but got $exception")
        assertEquals(500, exception.statusCode)
    }

    private fun testSessionRequest(displayName: String? = null) = SdkPostsSessionRequestDto(
        externalUserId = "u1",
        displayName = displayName,
        device = SdkPostsSessionDeviceDto(platform = "android", osVersion = "16"),
    )
}
