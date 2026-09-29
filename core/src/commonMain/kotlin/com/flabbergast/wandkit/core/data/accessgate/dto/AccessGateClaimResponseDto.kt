package com.flabbergast.wandkit.core.data.accessgate.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `POST /api/v1/sdk/access-gate/claim`. */
@Serializable
internal data class AccessGateClaimResponseDto(
    /** The normalized, display-formatted code, e.g. `K7QM-2XFT`. */
    @SerialName("code")
    val code: String,
    /** Total claims of this code, including this one. */
    @SerialName("claim_count")
    val claimCount: Int,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)
