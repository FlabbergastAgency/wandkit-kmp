package com.flabbergast.wandkit.core.data.posts

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreateAttachmentRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreateAttachmentResponseDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreatePostRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreatedPostDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionResponseDto
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.contentType

internal interface PostsApi {
    suspend fun createSession(request: SdkPostsSessionRequestDto): WandKitHttpResponse<SdkPostsSessionResponseDto>

    suspend fun createAttachment(
        token: String,
        request: SdkCreateAttachmentRequestDto,
    ): WandKitHttpResponse<SdkCreateAttachmentResponseDto>

    suspend fun createPost(
        token: String,
        request: SdkCreatePostRequestDto,
    ): WandKitHttpResponse<SdkCreatedPostDto>

    /**
     * PUTs the raw bytes to a mint response's `upload_url`. Not deserialized -
     * callers should check the status via [WandKitApi.status] rather than
     * [WandKitApi.invoke], since a presigned host answers with its own body
     * (or none at all), never ours to parse.
     *
     * [url] may be absolute (a presigned, third-party host - no [bearer] is
     * sent) or a path relative to the API base URL (a local/dev stack that
     * can't presign - [bearer] is sent so the upload can authenticate).
     */
    suspend fun uploadAttachment(
        url: String,
        bytes: ByteArray,
        contentType: String,
        bearer: String?,
    ): WandKitHttpResponse<Unit>
}

internal fun createPostsApi(
    httpClient: WandKitHttpClient,
    baseUrl: String,
    logger: Logger,
): WandKitApi<PostsApi> = WandKitApi(
    api = PostsApiImpl(httpClient.client, baseUrl),
    logger = logger,
)

private fun HttpRequestBuilder.bearerAuth(token: String) {
    headers {
        append(HttpHeaders.Authorization, "Bearer $token")
    }
}

private class PostsApiImpl(
    private val client: HttpClient,
    private val baseUrl: String,
) : PostsApi {
    override suspend fun createSession(
        request: SdkPostsSessionRequestDto,
    ): WandKitHttpResponse<SdkPostsSessionResponseDto> {
        val response = client.post("$baseUrl/api/v1/sdk/posts/session") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun createAttachment(
        token: String,
        request: SdkCreateAttachmentRequestDto,
    ): WandKitHttpResponse<SdkCreateAttachmentResponseDto> {
        val response = client.post("$baseUrl/api/v1/sdk/posts/attachments") {
            bearerAuth(token)
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun createPost(
        token: String,
        request: SdkCreatePostRequestDto,
    ): WandKitHttpResponse<SdkCreatedPostDto> {
        val response = client.post("$baseUrl/api/v1/sdk/posts") {
            bearerAuth(token)
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun uploadAttachment(
        url: String,
        bytes: ByteArray,
        contentType: String,
        bearer: String?,
    ): WandKitHttpResponse<Unit> {
        val isAbsolute = url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)
        val resolvedUrl = if (isAbsolute) url else baseUrl + (if (url.startsWith("/")) url else "/$url")
        val parsedContentType = ContentType.parse(contentType)

        val response = client.put(resolvedUrl) {
            // Explicit, not just via the body content: the client's
            // DefaultRequest forces `Content-Type: application/json` on every
            // request, and a header set here on the builder wins over that
            // default.
            contentType(parsedContentType)
            setBody(ByteArrayContent(bytes, parsedContentType))
            if (!isAbsolute && bearer != null) {
                bearerAuth(bearer)
            }
        }
        return WandKitHttpResponse(response)
    }
}
