package com.flabbergast.wandkit.core.data.accessgate.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `POST /api/v1/sdk/access-gate/status`. */
@Serializable
internal data class AccessGateStatusResponseDto(
    @SerialName("enabled")
    val enabled: Boolean,
    /** Dashboard-configured copy; `null` or blank falls back to the SDK's defaults. */
    @SerialName("title")
    val title: String? = null,
    @SerialName("message")
    val message: String? = null,
    /** "Need a code?" link target; `null` hides the link. */
    @SerialName("help_url")
    val helpUrl: String? = null,
    /** `null` when the request carried no `code`. */
    @SerialName("code")
    val code: AccessGateCodeStatusDto? = null,
)

@Serializable
internal data class AccessGateCodeStatusDto(
    /**
     * `active | expired | revoked | not_found`. Kept as a raw string so a
     * status the backend adds later still decodes; the repository maps
     * anything unknown to not found.
     */
    @SerialName("status")
    val status: String,
    @SerialName("claim_count")
    val claimCount: Int = 0,
)
