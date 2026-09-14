package com.flabbergast.wandkit.core.domain.featurepreview

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.posts.PostsApi
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.posts.PostsSessionRepository

private const val STATE_AVAILABLE = "available"

/** Sheet copy for a feature-preview state, dashboard-managed via the post's "Feature preview" section. */
internal data class FeaturePreviewCopy(
    val title: String,
    val message: String,
    val primaryLabel: String,
    val secondaryLabel: String,
)

/** Where a post's feature-preview state lands the flow. */
internal sealed interface FeaturePreviewResolution {
    data class Available(
        val postId: String,
        val postStatus: String,
        val previewCopy: FeaturePreviewCopy,
        /** `null` hides the primary button - no store URL configured for this project/platform. */
        val storeUrl: String?,
    ) : FeaturePreviewResolution

    data class ComingSoon(
        val postId: String,
        val postStatus: String,
        val previewCopy: FeaturePreviewCopy,
        val viewerVoted: Boolean,
    ) : FeaturePreviewResolution

    /**
     * No preview configured for the post, disabled, the post isn't
     * published, or the fetch (including the session mint) failed. Renders
     * as a generic "coming soon" fallback sheet rather than nothing.
     */
    data object Generic : FeaturePreviewResolution
}

/** The `status` value recorded on the SDK's `feature_preview_opened` event when [FeaturePreviewResolution.Generic] is shown. */
internal const val FEATURE_PREVIEW_STATUS_UNCONFIGURED = "unconfigured"

/** The `status` property recorded on the SDK's `feature_preview_opened` event - the post's status, or [FEATURE_PREVIEW_STATUS_UNCONFIGURED] for the generic fallback. */
internal fun FeaturePreviewResolution.statusForEvent(): String = when (this) {
    is FeaturePreviewResolution.Available -> postStatus
    is FeaturePreviewResolution.ComingSoon -> postStatus
    FeaturePreviewResolution.Generic -> FEATURE_PREVIEW_STATUS_UNCONFIGURED
}

/**
 * Fetches [postId]'s feature-preview state from the SDK endpoint the
 * dashboard's "Feature preview" section feeds: `available` once the post is
 * `done`, `coming_soon` for anything else published with a preview enabled.
 * A 404 (no preview configured, disabled, or unpublished) or any other fetch
 * failure (including a session mint failure) resolves to
 * [FeaturePreviewResolution.Generic] rather than surfacing an error.
 */
internal fun interface ResolveFeaturePreviewUseCase {
    suspend operator fun invoke(postId: String): FeaturePreviewResolution
}

internal fun createResolveFeaturePreviewUseCase(
    postsApi: WandKitApi<PostsApi>,
    postsSessionRepository: PostsSessionRepository,
    logger: Logger,
): ResolveFeaturePreviewUseCase = DefaultResolveFeaturePreviewUseCase(
    postsApi = postsApi,
    postsSessionRepository = postsSessionRepository,
    logger = logger,
)

private const val LOGGER_TAG = "[ResolveFeaturePreviewUseCase]"

private class DefaultResolveFeaturePreviewUseCase(
    private val postsApi: WandKitApi<PostsApi>,
    private val postsSessionRepository: PostsSessionRepository,
    private val logger: Logger,
) : ResolveFeaturePreviewUseCase {
    override suspend fun invoke(postId: String): FeaturePreviewResolution {
        val session = postsSessionRepository.mintSession().getOrElse {
            logger.warn(LOGGER_TAG, "Session mint failed for postId=$postId; feature preview generic fallback.", it)
            return FeaturePreviewResolution.Generic
        }

        val dto = postsApi { getFeaturePreview(session.token, postId) }.getOrElse {
            logger.debug(LOGGER_TAG, "Feature preview fetch failed for postId=$postId; generic fallback.", it)
            return FeaturePreviewResolution.Generic
        }.data

        val previewCopy = FeaturePreviewCopy(
            title = dto.previewCopy.title,
            message = dto.previewCopy.message,
            primaryLabel = dto.previewCopy.primaryLabel,
            secondaryLabel = dto.previewCopy.secondaryLabel,
        )

        return if (dto.state == STATE_AVAILABLE) {
            FeaturePreviewResolution.Available(
                postId = dto.postId,
                postStatus = dto.postStatus,
                previewCopy = previewCopy,
                storeUrl = dto.storeUrl,
            )
        } else {
            FeaturePreviewResolution.ComingSoon(
                postId = dto.postId,
                postStatus = dto.postStatus,
                previewCopy = previewCopy,
                viewerVoted = dto.viewerVoted,
            )
        }
    }
}
