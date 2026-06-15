package com.flabbergast.wandkit.core.domain.referrals

import kotlin.time.Instant

public data class ReferralInfo(
    public val referralId: String,
    public val code: String,
    public val shortPath: String,
    public val url: String,
    public val campaign: String,
    public val campaignName: String,
    public val campaignImageUrl: String?,
    public val projectName: String?,
    public val inviterId: String,
    public val status: String,
    public val usageMode: String,
    public val maxUses: Int?,
    public val claimedCount: Int,
    public val createdAt: Instant,
    public val expiresAt: Instant?,
    public val updatedAt: Instant,
)
