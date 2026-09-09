package com.flabbergast.wandkit.core.domain.screenshot

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.posts.PostsApi
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreateAttachmentRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkCreatePostRequestDto
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.posts.PostsSessionRepository
import com.flabbergast.wandkit.core.feedback.WandKitComposerAttachment
import com.flabbergast.wandkit.core.feedback.WandKitDebugAttachment
import com.flabbergast.wandkit.core.feedback.WandKitDebugAttachmentsProvider
import com.flabbergast.wandkit.core.feedback.WandKitPostType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

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
    debugAttachmentsProvider: () -> WandKitDebugAttachmentsProvider?,
    logger: Logger,
): SubmitScreenshotReportUseCase = DefaultSubmitScreenshotReportUseCase(
    postsApi = postsApi,
    postsSessionRepository = postsSessionRepository,
    debugAttachmentsProvider = debugAttachmentsProvider,
    logger = logger,
)

private const val LOGGER_TAG = "[SubmitScreenshotReportUseCase]"

/** Debug attachments provider's total time budget; on expiry the report proceeds without files. */
private const val DEBUG_PROVIDER_TIMEOUT_MILLIS = 10_000L

/** Kept files after caps are applied; extras are dropped with a warning. */
private const val MAX_DEBUG_ATTACHMENTS = 5

/** Per-file size cap; oversize files are dropped with a warning. */
private const val MAX_DEBUG_ATTACHMENT_BYTES = 10L * 1024 * 1024

/** The kind the API stores debug attachments under - dashboard-only, never shown to the end user. */
private const val DEBUG_ATTACHMENT_KIND = "debug"

/** Anonymous sessions can't write; the gate that publishes the prompt already keeps anonymous users out. */
internal class ReadOnlyPostsSessionException :
    Exception("This posts session is read-only and can't submit a report.")

private class DefaultSubmitScreenshotReportUseCase(
    private val postsApi: WandKitApi<PostsApi>,
    private val postsSessionRepository: PostsSessionRepository,
    private val debugAttachmentsProvider: () -> WandKitDebugAttachmentsProvider?,
    private val logger: Logger,
) : SubmitScreenshotReportUseCase {
    override suspend fun invoke(text: String, attachment: WandKitComposerAttachment): Result<String> = runCatching {
        val session = postsSessionRepository.mintSession().getOrThrow()
        if (session.readOnly) throw ReadOnlyPostsSessionException()

        val debugFiles = collectDebugAttachments()

        val screenshotId = mintAndUpload(
            token = session.token,
            kind = attachment.kind,
            contentType = attachment.contentType,
            data = attachment.data,
            fileName = attachment.fileName,
        ).getOrThrow()

        val debugIds = debugFiles.mapNotNull { file ->
            mintAndUpload(
                token = session.token,
                kind = DEBUG_ATTACHMENT_KIND,
                contentType = file.contentType,
                data = file.data,
                fileName = file.fileName,
            ).onFailure {
                logger.warn(LOGGER_TAG, "Debug attachment \"${file.fileName}\" skipped.", it)
            }.getOrNull()
        }

        val post = postsApi {
            createPost(
                session.token,
                SdkCreatePostRequestDto(
                    text = text,
                    type = WandKitPostType.BUG.wireValue,
                    attachmentIds = listOf(screenshotId) + debugIds,
                ),
            )
        }.getOrThrow().data

        post.id
    }.onFailure {
        logger.warn(LOGGER_TAG, "Screenshot report submission failed.", it)
    }

    /** Mints an upload slot for one attachment and PUTs its bytes. Shared by the screenshot and debug-file paths. */
    private suspend fun mintAndUpload(
        token: String,
        kind: String,
        contentType: String,
        data: ByteArray,
        fileName: String?,
    ): Result<String> = runCatching {
        val created = postsApi {
            createAttachment(
                token,
                SdkCreateAttachmentRequestDto(
                    kind = kind,
                    contentType = contentType,
                    sizeBytes = data.size.toLong(),
                    fileName = fileName,
                ),
            )
        }.getOrThrow().data

        postsApi.status {
            uploadAttachment(
                url = created.uploadUrl,
                bytes = data,
                contentType = contentType,
                bearer = token,
            )
        }.getOrThrow()

        created.id
    }

    /**
     * Runs the host's debug attachments provider (if any) under a time budget
     * and applies the file-count/size caps. Never throws - a slow or failing
     * provider just means the report goes out without debug files.
     */
    private suspend fun collectDebugAttachments(): List<WandKitDebugAttachment> {
        val provider = debugAttachmentsProvider() ?: return emptyList()

        val files = withTimeoutOrNull(DEBUG_PROVIDER_TIMEOUT_MILLIS) {
            try {
                provider.provide()
            } catch (e: CancellationException) {
                // The timeout (or an outer cancellation) - never swallow it.
                throw e
            } catch (e: Exception) {
                logger.warn(LOGGER_TAG, "Debug attachments provider failed.", e)
                emptyList()
            }
        }
        if (files == null) {
            logger.warn(LOGGER_TAG, "Debug attachments provider timed out after ${DEBUG_PROVIDER_TIMEOUT_MILLIS}ms.")
            return emptyList()
        }

        val withinSizeLimit = files.filter { file ->
            val fits = file.data.size.toLong() <= MAX_DEBUG_ATTACHMENT_BYTES
            if (!fits) {
                logger.warn(
                    LOGGER_TAG,
                    "Debug attachment \"${file.fileName}\" dropped: ${file.data.size} bytes exceeds the " +
                        "$MAX_DEBUG_ATTACHMENT_BYTES byte limit.",
                )
            }
            fits
        }

        if (withinSizeLimit.size > MAX_DEBUG_ATTACHMENTS) {
            logger.warn(
                LOGGER_TAG,
                "Debug attachments truncated to $MAX_DEBUG_ATTACHMENTS of ${withinSizeLimit.size}.",
            )
        }

        return withinSizeLimit.take(MAX_DEBUG_ATTACHMENTS)
    }
}
