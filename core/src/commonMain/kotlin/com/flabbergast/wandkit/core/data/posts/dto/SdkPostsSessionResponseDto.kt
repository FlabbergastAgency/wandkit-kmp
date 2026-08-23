package com.flabbergast.wandkit.core.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A minted posts-webview session: the bearer token the hosted web app calls
 * the API with, and the project configuration it renders from.
 */
@Serializable
internal data class SdkPostsSessionResponseDto(
    @SerialName("token")
    val token: String,
    /** ISO-8601, passed to the web app verbatim. */
    @SerialName("expires_at")
    val expiresAt: String,
    /**
     * True for an anonymous session. The web app renders the feed without any
     * of the write actions rather than letting them fail.
     */
    @SerialName("read_only")
    val readOnly: Boolean = false,
    /** The user's effective display name - their own if set, else the host's suggestion. */
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("config")
    val config: SdkPostsConfigDto,
)

@Serializable
internal data class SdkPostsConfigDto(
    /**
     * Kept as strings: the SDK never looks at them, and an unknown one added
     * server-side should reach the web app rather than fail to decode.
     */
    @SerialName("enabled_types")
    val enabledTypes: List<String> = emptyList(),
    @SerialName("roadmap_enabled")
    val roadmapEnabled: Boolean = false,
    @SerialName("tags")
    val tags: List<SdkPostsTagDto> = emptyList(),
)

@Serializable
internal data class SdkPostsTagDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: String? = null,
)
