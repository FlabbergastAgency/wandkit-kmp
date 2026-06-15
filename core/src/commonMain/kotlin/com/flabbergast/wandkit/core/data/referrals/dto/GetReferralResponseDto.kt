package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class GetReferralResponseDto(
    @SerialName("referral_id")
    val referralId: String,
    @SerialName("campaign")
    val campaign: String,
    @SerialName("campaign_name")
    val campaignName: String? = null,
    @SerialName("campaign_image_url")
    val campaignImageUrl: String? = null,
    @SerialName("project_name")
    val projectName: String? = null,
    @SerialName("properties")
    val properties: Map<String, JsonElement>? = null,
    @SerialName("status")
    val status: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
)
