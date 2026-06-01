package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class MatchedReferralDto(
    @SerialName("public_id")
    val publicId: String,
    @SerialName("campaign_key")
    val campaignKey: String,
    @SerialName("campaign_name")
    val campaignName: String? = null,
    @SerialName("campaign_image_url")
    val campaignImageUrl: String? = null,
    @SerialName("inviter_id")
    val inviterId: String,
    @SerialName("code")
    val code: String? = null,
    @SerialName("short_path")
    val shortPath: String,
    @SerialName("properties")
    val properties: Map<String, JsonElement>? = null,
    @SerialName("status")
    val status: String,
    @SerialName("usage_mode")
    val usageMode: String,
    @SerialName("max_uses")
    val maxUses: Int? = null,
    @SerialName("claimed_count")
    val claimedCount: Int,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)
