package com.flabbergast.wandkit.core.data.accessgate.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST /api/v1/sdk/access-gate/claim`. Always records a claim server-side,
 * including a repeat entry of the same code by the same install.
 */
@Serializable
internal data class AccessGateClaimRequestDto(
    @SerialName("install_id")
    val installId: String,
    /** As the user typed it, trimmed - the backend normalizes. */
    @SerialName("code")
    val code: String,
    @SerialName("platform")
    val platform: String,
    /** e.g. `android-0.1.8`, the same form the session replay header carries. */
    @SerialName("sdk_version")
    val sdkVersion: String,
    /**
     * The current anonymous id, the same one events carry as `device_id`.
     * Ties the claim to the anonymous user and, once the app identifies
     * someone, to that user (or straight to the user the backend already
     * linked this id to).
     */
    @SerialName("device_id")
    val deviceId: String,
    /** Only when the app has already identified the user; omitted otherwise, never made up. */
    @SerialName("external_user_id")
    val externalUserId: String? = null,
)
