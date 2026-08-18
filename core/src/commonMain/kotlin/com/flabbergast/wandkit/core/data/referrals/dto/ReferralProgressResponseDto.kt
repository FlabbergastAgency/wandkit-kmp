package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReferralProgressResponseDto(
    @SerialName("referral")
    val referral: CreateReferralResponseDto,
    @SerialName("claimed_count")
    val claimedCount: Int,
    @SerialName("converted_count")
    val convertedCount: Long,
    @SerialName("reward")
    val reward: ReferralRewardProgressDto,
)

@Serializable
internal data class ReferralRewardProgressDto(
    @SerialName("configured")
    val configured: Boolean,
    @SerialName("status")
    val status: String,
    @SerialName("threshold")
    val threshold: Int? = null,
    @SerialName("remaining")
    val remaining: Int? = null,
    @SerialName("entitlement_id")
    val entitlementId: String? = null,
    @SerialName("duration")
    val duration: String? = null,
    @SerialName("granted_at")
    val grantedAt: String? = null,
)
