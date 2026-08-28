package com.flabbergast.wandkit.core.domain.screenshot

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.posts.PostsApi
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreateAttachmentRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreatePostRequestDto
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.posts.PostsSessionRepository
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import com.flabbergast.wandkit.core.feedback.WandKitPostType

/**
 * Uploads a screenshot and creates the report post directly against the API,
 * bypassing the webview composer entirely. Mints a fresh, short-lived posts
 * session per send (tokens are in-memory by design).
 */
internal fun interface SubmitScreenshotReportUseCase {
    suspend operator fun invoke(text: String, attachment: WandKitComposerAttachment): Result<String>
}

internal fun createSubmitScreenshotReportUseCase(
    postsApi: WandKitApi<PostsApi>,
    postsSessionRepository: PostsSessionRepository,
    logger: Logger,
): SubmitScreenshotReportUseCase = DefaultSubmitScreenshotReportUseCase(
    postsApi = postsApi,
    postsSessionRepository = postsSessionRepository,
    logger = logger,
)

private const val LOGGER_TAG = "[SubmitScreenshotReportUseCase]"

/** Anonymous sessions can't write; the gate that publishes the prompt already keeps anonymous users out. */
internal class ReadOnlyPostsSessionException :
    Exception("This posts session is read-only and can't submit a report.")

private class DefaultSubmitScreenshotReportUseCase(
    private val postsApi: WandKitApi<PostsApi>,
    private val postsSessionRepository: PostsSessionRepository,
    private val logger: Logger,
) : SubmitScreenshotReportUseCase {
    override suspend fun invoke(text: String, attachment: WandKitComposerAttachment): Result<String> = runCatching {
        val session = postsSessionRepository.mintSession().getOrThrow()
        if (session.readOnly) throw ReadOnlyPostsSessionException()

        val createdAttachment = postsApi {
            createAttachment(
                session.token,
                SdkCreateAttachmentRequestDto(
                    kind = attachment.kind,
                    contentType = attachment.contentType,
                    sizeBytes = attachment.data.size.toLong(),
                ),
            )
        }.getOrThrow().data

        postsApi.status {
            uploadAttachment(
                url = createdAttachment.uploadUrl,
                bytes = attachment.data,
                contentType = attachment.contentType,
                bearer = session.token,
            )
        }.getOrThrow()

        val post = postsApi {
            createPost(
                session.token,
                SdkCreatePostRequestDto(
                    text = text,
                    type = WandKitPostType.BUG.wireValue,
                    attachmentIds = listOf(createdAttachment.id),
                ),
            )
        }.getOrThrow().data

        post.id
    }.onFailure {
        logger.warn(LOGGER_TAG, "Screenshot report submission failed.", it)
    }
}
