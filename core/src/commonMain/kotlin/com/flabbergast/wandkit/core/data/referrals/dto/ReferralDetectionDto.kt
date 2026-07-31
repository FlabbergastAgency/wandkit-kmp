package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ReferralDetectionDto(
    @SerialName("referral_id")
    val referralId: String,
    @SerialName("code")
    val code: String,
    @SerialName("campaign")
    val campaign: String,
    @SerialName("campaign_name")
    val campaignName: String? = null,
    @SerialName("campaign_image_url")
    val campaignImageUrl: String? = null,
    @SerialName("inviter_id")
    val inviterId: String,
    @SerialName("properties")
    val properties: Map<String, JsonElement>? = null,
)
