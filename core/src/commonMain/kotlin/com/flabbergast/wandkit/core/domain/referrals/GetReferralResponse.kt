package com.flabbergast.wandkit.core.domain.referrals

import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant

public data class GetReferralResponse(
    public val referralId: String,
    public val campaign: String,
    public val campaignName: String?,
    public val campaignImageUrl: String?,
    public val projectName: String?,
    public val properties: Map<String, String>?,
    public val status: String,
    public val expiresAt: Instant?,
)
