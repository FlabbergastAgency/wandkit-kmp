package com.flabbergast.wandkit.core.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Creates the report post directly. Field names are the API's (snake_case). */
@Serializable
internal data class SdkCreatePostRequestDto(
    @SerialName("text")
    val text: String,
    @SerialName("type")
    val type: String? = null,
    @SerialName("attachment_ids")
    val attachmentIds: List<String>? = null,
)

@Serializable
internal data class SdkCreatedPostDto(
    @SerialName("id")
    val id: String,
)
