package com.flabbergast.wandkit.core.domain.posts

import com.flabbergast.wandkit.core.data.networking.WandKitHttpException
import com.flabbergast.wandkit.core.feedback.WandKitPost
import com.flabbergast.wandkit.core.feedback.WandKitPostVote
import com.flabbergast.wandkit.core.feedback.WandKitReadOnlySessionException
import com.flabbergast.wandkit.core.testutil.NoOpLogger
import com.flabbergast.wandkit.core.testutil.createTestPostsApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostVotesUseCaseTest {
    @Test
    fun postMapsTheBodyAndTheSessionsWriteAccess() = runTest {
        val engine = createPostEngine()
        val useCase = useCase(engine, readOnly = false)

        val result = useCase.post("post-1")

        assertEquals(
            WandKitPost(
                id = "post-1",
                title = "Dark mode",
                status = "planned",
                voteCount = 12,
                viewerVoted = false,
                canVote = true,
            ),
            result.getOrThrow(),
        )
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/v1/sdk/posts/post-1", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun postOnAReadOnlySessionStillReadsButCannotVote() = runTest {
        val useCase = useCase(createPostEngine(), readOnly = true)

        val post = useCase.post("post-1").getOrThrow()

        assertEquals(false, post.canVote)
        assertEquals(12, post.voteCount)
    }

    @Test
    fun postThatIsNotFoundFailsWith404() = runTest {
        val useCase = useCase(MockEngine { respond("", HttpStatusCode.NotFound) }, readOnly = false)

        val exception = useCase.post("missing").exceptionOrNull()

        assertTrue(exception is WandKitHttpException, "Expected WandKitHttpException but got $exception")
        assertEquals(404, exception.statusCode)
    }

    @Test
    fun setVoteTruePutsTheVote() = runTest {
        val engine = createVoteEngine()
        val useCase = useCase(engine, readOnly = false)

        val result = useCase.setVote("post-1", voted = true)

        assertEquals(WandKitPostVote(voteCount = 13, viewerVoted = true), result.getOrThrow())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/api/v1/sdk/posts/post-1/vote", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun setVoteFalseDeletesTheVote() = runTest {
        val engine = createVoteEngine()
        val useCase = useCase(engine, readOnly = false)

        val result = useCase.setVote("post-1", voted = false)

        assertEquals(WandKitPostVote(voteCount = 12, viewerVoted = false), result.getOrThrow())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/sdk/posts/post-1/vote", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun setVoteOnAReadOnlySessionFailsWithoutARequest() = runTest {
        val engine = createVoteEngine()
        val useCase = useCase(engine, readOnly = true)

        val result = useCase.setVote("post-1", voted = true)

        assertTrue(result.exceptionOrNull() is WandKitReadOnlySessionException)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun setVoteRefusedByTheServerFailsWith403() = runTest {
        val useCase = useCase(MockEngine { respond("", HttpStatusCode.Forbidden) }, readOnly = false)

        val exception = useCase.setVote("post-1", voted = true).exceptionOrNull()

        assertTrue(exception is WandKitHttpException, "Expected WandKitHttpException but got $exception")
        assertEquals(403, exception.statusCode)
    }

    @Test
    fun aFailedSessionMintFailsTheCall() = runTest {
        val engine = createVoteEngine()
        val mintError = IllegalStateException("offline")
        val useCase = createPostVotesUseCase(
            postsApi = createTestPostsApi(engine),
            postsSessionRepository = object : PostsSessionRepository {
                override suspend fun mintSession(): Result<PostsSession> = Result.failure(mintError)
            },
            logger = NoOpLogger,
        )

        assertEquals(mintError, useCase.post("post-1").exceptionOrNull())
        assertEquals(mintError, useCase.setVote("post-1", voted = true).exceptionOrNull())
        assertTrue(engine.requestHistory.isEmpty())
    }

    private fun useCase(engine: MockEngine, readOnly: Boolean) = createPostVotesUseCase(
        postsApi = createTestPostsApi(engine),
        postsSessionRepository = FakePostsSessionRepository(readOnly = readOnly),
        logger = NoOpLogger,
    )
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private fun createPostEngine() = MockEngine {
    respond(
        content = """{"id":"post-1","title":"Dark mode","status":"planned","vote_count":12,"viewer_voted":false,"type":"bug"}""",
        status = HttpStatusCode.OK,
        headers = jsonHeaders,
    )
}

/** PUT answers the voted state, DELETE the unvoted one. */
private fun createVoteEngine() = MockEngine { request ->
    val voted = request.method == HttpMethod.Put
    respond(
        content = """{"vote_count":${if (voted) 13 else 12},"viewer_voted":$voted}""",
        status = HttpStatusCode.OK,
        headers = jsonHeaders,
    )
}

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
