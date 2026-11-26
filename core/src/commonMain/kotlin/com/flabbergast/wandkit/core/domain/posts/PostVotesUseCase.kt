package com.flabbergast.wandkit.core.domain.posts

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.posts.PostsApi
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.feedback.WandKitPost
import com.flabbergast.wandkit.core.feedback.WandKitPostVote
import com.flabbergast.wandkit.core.feedback.WandKitReadOnlySessionException
import kotlinx.coroutines.CancellationException

/**
 * Reads one post and sets the viewer's vote on it, without any WandKit UI -
 * for host apps that show a post as a card in their own screens. Mints a
 * fresh posts session per call, like `SubmitPostUseCase`.
 */
internal interface PostVotesUseCase {
    suspend fun post(postId: String): Result<WandKitPost>

    suspend fun setVote(postId: String, voted: Boolean): Result<WandKitPostVote>
}

internal fun createPostVotesUseCase(
    postsApi: WandKitApi<PostsApi>,
    postsSessionRepository: PostsSessionRepository,
    logger: Logger,
): PostVotesUseCase = DefaultPostVotesUseCase(
    postsApi = postsApi,
    postsSessionRepository = postsSessionRepository,
    logger = logger,
)

private const val LOGGER_TAG = "[PostVotes]"

private class DefaultPostVotesUseCase(
    private val postsApi: WandKitApi<PostsApi>,
    private val postsSessionRepository: PostsSessionRepository,
    private val logger: Logger,
) : PostVotesUseCase {
    override suspend fun post(postId: String): Result<WandKitPost> = runCatching {
        val session = postsSessionRepository.mintSession().getOrThrow()
        val post = postsApi { getPost(session.token, postId) }.getOrThrow().data

        WandKitPost(
            id = post.id,
            title = post.title,
            status = post.status,
            voteCount = post.voteCount,
            viewerVoted = post.viewerVoted,
            canVote = !session.readOnly,
        )
    }.onFailure {
        // runCatching also catches the caller's cancellation; it must keep propagating.
        if (it is CancellationException) throw it
        logger.warn(LOGGER_TAG, "Reading postId=$postId failed.", it)
    }

    override suspend fun setVote(postId: String, voted: Boolean): Result<WandKitPostVote> = runCatching {
        val session = postsSessionRepository.mintSession().getOrThrow()
        if (session.readOnly) throw WandKitReadOnlySessionException()

        val state = postsApi {
            if (voted) votePost(session.token, postId) else unvotePost(session.token, postId)
        }.getOrThrow().data

        WandKitPostVote(voteCount = state.voteCount, viewerVoted = state.viewerVoted)
    }.onFailure {
        if (it is CancellationException) throw it
        logger.warn(LOGGER_TAG, "Setting the vote (voted=$voted) on postId=$postId failed.", it)
    }
}
