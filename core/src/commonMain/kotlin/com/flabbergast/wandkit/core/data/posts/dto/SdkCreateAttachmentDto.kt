package com.flabbergast.wandkit.core.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mints an upload slot for one attachment. Field names are the API's (snake_case). */
@Serializable
internal data class SdkCreateAttachmentRequestDto(
    @SerialName("kind")
    val kind: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("size_bytes")
    val sizeBytes: Long,
)

@Serializable
internal data class SdkCreateAttachmentResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("upload_url")
    val uploadUrl: String,
)
