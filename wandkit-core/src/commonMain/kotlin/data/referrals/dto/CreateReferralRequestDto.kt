package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class CreateReferralRequestDto(
    @SerialName("campaign_key")
    val campaignKey: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("properties")
    val properties: Map<String, JsonElement>? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("usage_mode")
    val usageMode: String? = null,
    @SerialName("max_uses")
    val maxUses: Int? = null,
)
