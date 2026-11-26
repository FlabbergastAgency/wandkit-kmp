package com.flabbergast.wandkit.core.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The fields of the API's SDKPost the native post card needs; the rest are ignored. */
@Serializable
internal data class SdkPostDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String = "",
    @SerialName("status")
    val status: String = "",
    @SerialName("vote_count")
    val voteCount: Int = 0,
    @SerialName("viewer_voted")
    val viewerVoted: Boolean = false,
)
