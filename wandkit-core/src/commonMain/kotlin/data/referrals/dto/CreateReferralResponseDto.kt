package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class CreateReferralResponseDto(
    @SerialName("referral_id")
    val referralId: String,
    @SerialName("code")
    val code: String,
    @SerialName("short_path")
    val shortPath: String,
    @SerialName("url")
    val url: String,
    @SerialName("campaign")
    val campaign: String,
    @SerialName("campaign_name")
    val campaignName: String,
    @SerialName("campaign_image_url")
    val campaignImageUrl: String? = null,
    @SerialName("project_name")
    val projectName: String? = null,
    @SerialName("properties")
    val properties: Map<String, JsonElement>? = null,
    @SerialName("inviter_id")
    val inviterId: String,
    @SerialName("status")
    val status: String,
    @SerialName("usage_mode")
    val usageMode: String,
    @SerialName("max_uses")
    val maxUses: Int? = null,
    @SerialName("claimed_count")
    val claimedCount: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String,
)
