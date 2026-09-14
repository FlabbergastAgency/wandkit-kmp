package com.flabbergast.wandkit.core.domain.featurepreview

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.posts.PostsApi
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.posts.PostsSessionRepository
import com.flabbergast.wandkit.core.domain.screenshot.ReadOnlyPostsSessionException

/**
 * Votes (and therefore follows) the post behind a coming-soon sheet. Mints a
 * fresh, short-lived posts session per call - same pattern as
 * `SubmitScreenshotReportUseCase` - rather than reusing whatever token the
 * initial fetch happened to use.
 */
internal fun interface VoteFeaturePreviewPostUseCase {
    suspend operator fun invoke(postId: String): Result<Unit>
}

internal fun createVoteFeaturePreviewPostUseCase(
    postsApi: WandKitApi<PostsApi>,
    postsSessionRepository: PostsSessionRepository,
    logger: Logger,
): VoteFeaturePreviewPostUseCase = DefaultVoteFeaturePreviewPostUseCase(
    postsApi = postsApi,
    postsSessionRepository = postsSessionRepository,
    logger = logger,
)

private const val LOGGER_TAG = "[VoteFeaturePreviewPostUseCase]"

private class DefaultVoteFeaturePreviewPostUseCase(
    private val postsApi: WandKitApi<PostsApi>,
    private val postsSessionRepository: PostsSessionRepository,
    private val logger: Logger,
) : VoteFeaturePreviewPostUseCase {
    override suspend fun invoke(postId: String): Result<Unit> = runCatching {
        val session = postsSessionRepository.mintSession().getOrThrow()
        if (session.readOnly) throw ReadOnlyPostsSessionException()

        postsApi { votePost(session.token, postId) }.getOrThrow()

        Unit
    }.onFailure {
        logger.warn(LOGGER_TAG, "Feature preview vote failed for postId=$postId.", it)
    }
}
