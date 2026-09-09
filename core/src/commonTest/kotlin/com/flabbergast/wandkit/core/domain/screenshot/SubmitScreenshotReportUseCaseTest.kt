package com.flabbergast.wandkit.core.domain.screenshot

import com.flabbergast.wandkit.core.domain.posts.PostsConfig
import com.flabbergast.wandkit.core.domain.posts.PostsSession
import com.flabbergast.wandkit.core.domain.posts.PostsSessionRepository
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import com.flabbergast.wandkit.core.feedback.WandKitDebugAttachment
import com.flabbergast.wandkit.core.feedback.WandKitDebugAttachmentsProvider
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import com.flabbergast.wandkit.core.testutil.createTestPostsApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubmitScreenshotReportUseCaseTest {
    private fun screenshotAttachment() = WandKitComposerAttachment(
        data = byteArrayOf(1, 2, 3),
        contentType = "image/png",
        fileName = "screenshot.png",
    )

    private fun session(readOnly: Boolean = false) = FakePostsSessionRepository(readOnly = readOnly)

    @Test
    fun happyPathWithOneDebugFileUploadsBothAndCreatesPostWithBothIds() = runTest {
        val engine = createRoutedEngine()
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(),
            debugAttachmentsProvider = {
                WandKitDebugAttachmentsProvider { listOf(WandKitDebugAttachment.text("log line", "app.log")) }
            },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isSuccess, "Expected success but got $result")
        assertEquals("post-1", result.getOrNull())

        val requests = engine.requestHistory
        assertEquals(5, requests.size, "Expected 5 requests but got ${requests.map { it.url.encodedPath }}")

        val (mintImage, putImage, mintDebug, putDebug, createPost) = requests

        assertEquals("/api/v1/sdk/posts/attachments", mintImage.url.encodedPath)
        assertEquals(HttpMethod.Put, putImage.method)

        assertEquals("/api/v1/sdk/posts/attachments", mintDebug.url.encodedPath)
        val debugMintBody = mintDebug.bodyText()
        assertTrue(debugMintBody.contains(""""kind":"debug""""), "Expected kind in $debugMintBody")
        assertTrue(debugMintBody.contains(""""file_name":"app.log""""), "Expected file_name in $debugMintBody")

        assertEquals(HttpMethod.Put, putDebug.method)
        assertEquals(ContentType.parse("text/plain; charset=utf-8"), putDebug.body.contentType)
        assertNull(putDebug.headers[HttpHeaders.Authorization], "Did not expect Authorization on an absolute upload URL")

        assertEquals("/api/v1/sdk/posts", createPost.url.encodedPath)
        val postBody = createPost.bodyText()
        assertTrue(postBody.contains(""""attachment_ids":["att-1","att-2"]"""), "Expected both ids in order in $postBody")
    }

    @Test
    fun debugMintFailureStillCreatesPostWithOnlyTheScreenshotId() = runTest {
        val engine = createRoutedEngine(debugMintStatus = HttpStatusCode.InternalServerError)
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(),
            debugAttachmentsProvider = {
                WandKitDebugAttachmentsProvider { listOf(WandKitDebugAttachment.text("log line", "app.log")) }
            },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isSuccess, "Expected success but got $result")
        val createPost = engine.requestHistory.last { it.url.encodedPath == "/api/v1/sdk/posts" }
        assertTrue(
            createPost.bodyText().contains(""""attachment_ids":["att-1"]"""),
            "Expected only the screenshot id in ${createPost.bodyText()}",
        )
    }

    @Test
    fun providerThrowingLeavesOnlyTheScreenshotId() = runTest {
        val engine = createRoutedEngine()
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(),
            debugAttachmentsProvider = { WandKitDebugAttachmentsProvider { throw IllegalStateException("boom") } },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isSuccess, "Expected success but got $result")
        val createPost = engine.requestHistory.last { it.url.encodedPath == "/api/v1/sdk/posts" }
        val body = createPost.bodyText()
        assertTrue(body.contains(""""attachment_ids":["att-1"]"""), "Expected only att-1 in $body")
    }

    @Test
    fun providerTimingOutLeavesOnlyTheScreenshotId() = runTest {
        val engine = createRoutedEngine()
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(),
            debugAttachmentsProvider = {
                WandKitDebugAttachmentsProvider {
                    delay(60_000)
                    listOf(WandKitDebugAttachment.text("log line", "app.log"))
                }
            },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isSuccess, "Expected success but got $result")
        val createPost = engine.requestHistory.last { it.url.encodedPath == "/api/v1/sdk/posts" }
        val body = createPost.bodyText()
        assertTrue(body.contains(""""attachment_ids":["att-1"]"""), "Expected only att-1 in $body")
    }

    @Test
    fun sevenFilesAreCappedToFiveDebugMints() = runTest {
        val engine = createRoutedEngine()
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(),
            debugAttachmentsProvider = {
                WandKitDebugAttachmentsProvider {
                    (1..7).map { WandKitDebugAttachment.text("log $it", "app-$it.log") }
                }
            },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isSuccess, "Expected success but got $result")
        val debugMints = engine.requestHistory.count {
            it.url.encodedPath == "/api/v1/sdk/posts/attachments" && it.bodyText().contains(""""kind":"debug"""")
        }
        assertEquals(5, debugMints)
    }

    @Test
    fun anOversizeFileIsSkippedEntirely() = runTest {
        val engine = createRoutedEngine()
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(),
            debugAttachmentsProvider = {
                WandKitDebugAttachmentsProvider {
                    listOf(
                        WandKitDebugAttachment(
                            data = ByteArray(11 * 1024 * 1024),
                            fileName = "huge.bin",
                            contentType = "application/octet-stream",
                        ),
                    )
                }
            },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isSuccess, "Expected success but got $result")
        val debugMints = engine.requestHistory.count {
            it.url.encodedPath == "/api/v1/sdk/posts/attachments" && it.bodyText().contains(""""kind":"debug"""")
        }
        assertEquals(0, debugMints, "Did not expect a mint request for the oversize file")
    }

    @Test
    fun readOnlySessionFailsWithoutAnyAttachmentRequests() = runTest {
        val engine = createRoutedEngine()
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(readOnly = true),
            debugAttachmentsProvider = {
                WandKitDebugAttachmentsProvider { listOf(WandKitDebugAttachment.text("log line", "app.log")) }
            },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isFailure, "Expected failure but got $result")
        assertTrue(engine.requestHistory.isEmpty(), "Did not expect any attachment requests")
    }

    @Test
    fun noProviderConfiguredMatchesTodaysThreeRequests() = runTest {
        val engine = createRoutedEngine()
        val useCase = createSubmitScreenshotReportUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = session(),
            debugAttachmentsProvider = { null },
            logger = NoOpLogger,
        )

        val result = useCase("It crashes", screenshotAttachment())

        assertTrue(result.isSuccess, "Expected success but got $result")
        assertEquals(3, engine.requestHistory.size)
    }
}

private fun HttpRequestData.bodyText(): String = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

/**
 * Routes by path + method, session-independent: every attachment mint returns
 * an incrementing `att-N` id, every PUT succeeds, and the post is always
 * created as `post-1`. [debugMintStatus] lets a test force the debug mint
 * (identified by `"kind":"debug"` in the body) to fail without touching the
 * screenshot mint.
 */
private fun createRoutedEngine(debugMintStatus: HttpStatusCode = HttpStatusCode.Created): MockEngine {
    var mintCount = 0
    return MockEngine { request ->
        when {
            request.method == HttpMethod.Post && request.url.encodedPath == "/api/v1/sdk/posts/attachments" -> {
                mintCount += 1
                val isDebug = request.bodyText().contains(""""kind":"debug"""")
                if (isDebug && debugMintStatus != HttpStatusCode.Created) {
                    respondEmpty(debugMintStatus)
                } else {
                    respond(
                        content = """{"id":"att-$mintCount","upload_url":"https://storage.test/att-$mintCount"}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
            request.method == HttpMethod.Put -> respondEmpty(HttpStatusCode.OK)
            request.method == HttpMethod.Post && request.url.encodedPath == "/api/v1/sdk/posts" -> respond(
                content = """{"id":"post-1"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
            else -> respondEmpty(HttpStatusCode.NotFound)
        }
    }
}

private fun MockRequestHandleScope.respondEmpty(status: HttpStatusCode) = respond(content = "", status = status)

private class FakePostsSessionRepository(private val readOnly: Boolean) : PostsSessionRepository {
    override suspend fun mintSession(): Result<PostsSession> = Result.success(
        PostsSession(
            token = "session-token",
            expiresAt = "2026-08-21T10:00:00Z",
            readOnly = readOnly,
            config = PostsConfig(enabledTypes = listOf("bug"), roadmapEnabled = false, tags = emptyList()),
        ),
    )
}
