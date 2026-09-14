package com.flabbergast.wandkit.core.domain.featurepreview

import com.flabbergast.wandkit.core.config.createAppConfiguration
import com.flabbergast.wandkit.core.data.posts.createPostsSessionRepository
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import com.flabbergast.wandkit.core.testutil.createTestPostsApi
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val SESSION_JSON = """{"token":"tok","expires_at":"2026-08-21T10:00:00Z","config":{}}"""

private const val COMING_SOON_COPY_JSON = """
    "copy":{"title":"FAA support is almost here","message":"Coming soon.",
    "primary_label":"Let me know when FAA is ready","secondary_label":"Continue with EASA"}
"""

class ResolveFeaturePreviewUseCaseTest {
    @Test
    fun availableStateResolvesToAvailableWithStoreUrl() = runBlocking {
        val useCase = useCaseRoutedBy { path ->
            when {
                path.endsWith("/session") -> jsonBody(SESSION_JSON)
                else -> jsonBody(
                    """
                    {"state":"available","post_id":"post-1","post_status":"done","viewer_voted":false,
                    $COMING_SOON_COPY_JSON,"store_url":"https://play.google.com/store/apps/details?id=com.example"}
                    """.trimIndent().replace("\n", ""),
                )
            }
        }

        val resolution = useCase("post-1")

        val available = assertIs<FeaturePreviewResolution.Available>(resolution)
        assertEquals("post-1", available.postId)
        assertEquals("done", available.postStatus)
        assertEquals("https://play.google.com/store/apps/details?id=com.example", available.storeUrl)
        assertEquals("FAA support is almost here", available.previewCopy.title)
    }

    @Test
    fun comingSoonStateResolvesToComingSoonWithViewerVoted() = runBlocking {
        val useCase = useCaseRoutedBy { path ->
            when {
                path.endsWith("/session") -> jsonBody(SESSION_JSON)
                else -> jsonBody(
                    """
                    {"state":"coming_soon","post_id":"post-1","post_status":"planned","viewer_voted":true,
                    $COMING_SOON_COPY_JSON,"store_url":null}
                    """.trimIndent().replace("\n", ""),
                )
            }
        }

        val resolution = useCase("post-1")

        val comingSoon = assertIs<FeaturePreviewResolution.ComingSoon>(resolution)
        assertEquals("post-1", comingSoon.postId)
        assertEquals("planned", comingSoon.postStatus)
        assertTrue(comingSoon.viewerVoted)
        assertEquals("Continue with EASA", comingSoon.previewCopy.secondaryLabel)
    }

    @Test
    fun notFoundResolvesToGeneric() = runBlocking {
        val useCase = useCaseRoutedBy { path ->
            when {
                path.endsWith("/session") -> jsonBody(SESSION_JSON)
                else -> respond(content = "", status = HttpStatusCode.NotFound)
            }
        }

        val resolution = useCase("post-1")

        assertEquals(FeaturePreviewResolution.Generic, resolution)
    }

    @Test
    fun networkErrorResolvesToGeneric() = runBlocking {
        val useCase = useCaseRoutedBy { path ->
            when {
                path.endsWith("/session") -> jsonBody(SESSION_JSON)
                else -> respond(content = "", status = HttpStatusCode.InternalServerError)
            }
        }

        val resolution = useCase("post-1")

        assertEquals(FeaturePreviewResolution.Generic, resolution)
    }

    @Test
    fun sessionMintFailureResolvesToGeneric() = runBlocking {
        val useCase = useCaseRoutedBy { _ ->
            respond(content = "", status = HttpStatusCode.Forbidden)
        }

        val resolution = useCase("post-1")

        assertEquals(FeaturePreviewResolution.Generic, resolution)
    }

    @Test
    fun genericResolutionReportsUnconfiguredStatusForTheOpenedEvent() {
        assertEquals(FEATURE_PREVIEW_STATUS_UNCONFIGURED, FeaturePreviewResolution.Generic.statusForEvent())
    }

    private fun MockRequestHandleScope.jsonBody(content: String): HttpResponseData = respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private fun useCaseRoutedBy(
        handler: MockRequestHandleScope.(path: String) -> HttpResponseData,
    ): ResolveFeaturePreviewUseCase {
        val postsApi = createTestPostsApi { request -> handler(request.url.encodedPath) }
        val postsSessionRepository = createPostsSessionRepository(
            postsApi = postsApi,
            appConfiguration = createAppConfiguration(isDebugLoggingEnabled = false, apiBaseUrl = "https://example.test"),
            platformContext = null,
            externalUserId = { "u1" },
            displayName = { null },
            logger = NoOpLogger,
        )

        return createResolveFeaturePreviewUseCase(
            postsApi = postsApi,
            postsSessionRepository = postsSessionRepository,
            logger = NoOpLogger,
        )
    }
}
