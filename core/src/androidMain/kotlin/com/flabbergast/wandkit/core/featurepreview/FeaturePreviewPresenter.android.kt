package com.flabbergast.wandkit.core.featurepreview

import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import com.flabbergast.wandkit.core.domain.featurepreview.FeaturePreviewCopy
import com.flabbergast.wandkit.core.domain.featurepreview.FeaturePreviewPrompt
import com.flabbergast.wandkit.core.domain.featurepreview.FeaturePreviewResolution
import com.flabbergast.wandkit.core.domain.featurepreview.statusForEvent
import kotlin.time.Clock

private const val EVENT_OPENED = "feature_preview_opened"

/** Fixed copy for [FeaturePreviewResolution.Generic] - there is no dashboard-managed copy to show, so it isn't fetched. */
private val GENERIC_PREVIEW_COPY = FeaturePreviewCopy(
    title = "This feature is coming soon",
    message = "We're still working on it. Check back in a future update.",
    primaryLabel = "Close",
    secondaryLabel = "",
)

internal actual fun presentFeaturePreviewFlow(
    container: WandKitSdkContainer,
    postId: String,
    onResult: (WandKitFeaturePreviewResult) -> Unit,
) {
    container.fireAndForgetTask {
        val resolution = container.resolveFeaturePreviewUseCase(postId)

        // Same drop-the-form rule as the controller's own recordEvent: a
        // returned form must not cover the sheet we are about to publish.
        container.eventsRepository.trackEvent(
            WandKitEvent(
                name = EVENT_OPENED,
                properties = mapOf("post_id" to postId, "status" to resolution.statusForEvent()),
                occurredAt = Clock.System.now(),
            ),
            container.identityInfo,
        )

        container.featurePreviewController.publish(resolution.toPrompt(requestedPostId = postId), onResult)
    }
}

/**
 * The resolution's own post id for [FeaturePreviewResolution.Available]/[FeaturePreviewResolution.ComingSoon] -
 * not [requestedPostId], since the endpoint may resolve a duplicate to its redirect target. [FeaturePreviewResolution.Generic]
 * carries no post id of its own (the fetch never got that far), so it falls back to [requestedPostId].
 */
private fun FeaturePreviewResolution.toPrompt(requestedPostId: String): FeaturePreviewPrompt = when (this) {
    is FeaturePreviewResolution.Available -> FeaturePreviewPrompt(
        postId = postId,
        previewCopy = previewCopy,
        state = FeaturePreviewPrompt.State.Available(storeUrl = storeUrl),
    )

    is FeaturePreviewResolution.ComingSoon -> FeaturePreviewPrompt(
        postId = postId,
        previewCopy = previewCopy,
        state = FeaturePreviewPrompt.State.ComingSoon(viewerVoted = viewerVoted),
    )

    FeaturePreviewResolution.Generic -> FeaturePreviewPrompt(
        postId = requestedPostId,
        previewCopy = GENERIC_PREVIEW_COPY,
        state = FeaturePreviewPrompt.State.Generic,
    )
}
