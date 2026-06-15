package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReferralMatchResponseDto(
    @SerialName("referral_id")
    val referralId: String,
    @SerialName("install_id")
    val installId: String,
    @SerialName("claim_method")
    val claimMethod: String,
    @SerialName("claimed_at")
    val claimedAt: String,
    @SerialName("referral")
    val referral: MatchedReferralDto,
)
