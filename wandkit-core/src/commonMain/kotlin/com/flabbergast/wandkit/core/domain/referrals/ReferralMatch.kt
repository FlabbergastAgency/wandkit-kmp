package com.flabbergast.wandkit.core.domain.referrals

import kotlin.time.Instant

public data class ReferralMatch(
    public val referralId: String,
    public val installId: String,
    public val claimMethod: String,
    public val claimedAt: Instant,
    public val inviterId: String,
    public val campaign: String,
    public val campaignName: String?,
    public val code: String?,
    public val shortPath: String,
    public val properties: Map<String, String>,
)
