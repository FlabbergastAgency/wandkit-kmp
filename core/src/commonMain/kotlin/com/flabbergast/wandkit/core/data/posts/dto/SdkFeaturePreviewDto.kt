package com.flabbergast.wandkit.core.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The feature-preview endpoint's response: dashboard-managed copy for a
 * post's "coming soon / it's here" sheet, plus whichever bit of post state
 * the flow needs. `ignoreUnknownKeys` on the shared [kotlinx.serialization.json.Json]
 * lets an unmodelled field (or an unrecognised `state`) reach here without
 * failing to decode - an unrecognised `state` is simply not `"available"`,
 * so [com.flabbergast.wandkit.core.domain.featurepreview.ResolveFeaturePreviewUseCase]
 * treats it the same as `"coming_soon"`.
 */
@Serializable
internal data class SdkFeaturePreviewDto(
    @SerialName("state")
    val state: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("post_status")
    val postStatus: String,
    @SerialName("viewer_voted")
    val viewerVoted: Boolean = false,
    @SerialName("copy")
    val previewCopy: SdkFeaturePreviewCopyDto,
    /** `null` when no store URL is configured for this project/platform - hides the available-state primary button. */
    @SerialName("store_url")
    val storeUrl: String? = null,
)

@Serializable
internal data class SdkFeaturePreviewCopyDto(
    @SerialName("title")
    val title: String,
    @SerialName("message")
    val message: String,
    @SerialName("primary_label")
    val primaryLabel: String,
    @SerialName("secondary_label")
    val secondaryLabel: String,
)
