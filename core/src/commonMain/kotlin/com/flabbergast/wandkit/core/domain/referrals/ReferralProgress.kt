package com.flabbergast.wandkit.core.domain.referrals

import kotlin.time.Instant

/**
 * How far an inviter is toward their reward - what drives an in-app
 * "3 of 5 friends joined" meter.
 */
public data class ReferralProgress(
    public val referral: ReferralInfo,
    /** Installs that entered this inviter's code. */
    public val claimedCount: Int,
    /** Claims that went on to sign up. This is what counts toward the reward. */
    public val convertedCount: Long,
    public val reward: ReferralRewardProgress,
)

public data class ReferralRewardProgress(
    /**
     * Whether this campaign is currently offering a reward. False when none is
     * set up or it has been switched off - in both cases conversions still
     * accumulate but nothing will be granted.
     */
    public val configured: Boolean,
    /** `none` means not yet earned; the rest mirror delivery of the earned grant. */
    public val status: String,
    /** Conversions needed to earn the reward. Null when not configured. */
    public val threshold: Int? = null,
    /** Conversions still needed, floored at zero. Null when not configured. */
    public val remaining: Int? = null,
    public val entitlementId: String? = null,
    public val duration: String? = null,
    /** When the reward was delivered. Null until then. */
    public val grantedAt: Instant? = null,
)
