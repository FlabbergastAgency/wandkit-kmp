package com.flabbergast.wandkit.core.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The current vote state, as returned by both vote and unvote. */
@Serializable
internal data class SdkPostVoteResponseDto(
    @SerialName("vote_count")
    val voteCount: Int = 0,
    @SerialName("viewer_voted")
    val viewerVoted: Boolean = false,
)
