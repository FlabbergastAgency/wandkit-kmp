package com.flabbergast.wandkit.core.domain.referrals

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A referral this install probably came from.
 *
 * Nothing is bound to it: the code is meant to be offered back to the user to
 * confirm or replace, and [com.flabbergast.wandkit.core.WandKit.redeemCode] is
 * what actually claims it.
 *
 * Serializable so the SDK can hold on to a detection until the app gets round to
 * asking the user about it, which may be several launches later.
 */
@Serializable
public data class ReferralDetection(
    @SerialName("referral_id")
    public val referralId: String,
    /** Prefill this in your invite-code field. */
    @SerialName("code")
    public val code: String,
    @SerialName("campaign")
    public val campaign: String,
    @SerialName("campaign_name")
    public val campaignName: String? = null,
    @SerialName("campaign_image_url")
    public val campaignImageUrl: String? = null,
    @SerialName("inviter_id")
    public val inviterId: String,
    @SerialName("properties")
    public val properties: Map<String, String> = emptyMap(),
)
