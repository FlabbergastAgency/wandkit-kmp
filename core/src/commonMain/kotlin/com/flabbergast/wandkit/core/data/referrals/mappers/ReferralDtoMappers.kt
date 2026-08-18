package com.flabbergast.wandkit.core.data.referrals.mappers

import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.GetReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralDetectionDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralProgressResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralRewardProgressDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchResponseDto
import com.flabbergast.wandkit.core.domain.referrals.GetReferralResponse
import com.flabbergast.wandkit.core.domain.referrals.ReferralDetection
import com.flabbergast.wandkit.core.domain.referrals.ReferralInfo
import com.flabbergast.wandkit.core.domain.referrals.ReferralProgress
import com.flabbergast.wandkit.core.domain.referrals.ReferralRewardProgress
import com.flabbergast.wandkit.core.domain.referrals.ReferralMatch
import kotlinx.serialization.json.Json

internal fun CreateReferralResponseDto.toReferralInfo(): ReferralInfo? =
    ReferralInfo(
        referralId = referralId,
        code = code,
        shortPath = shortPath,
        url = url,
        campaign = campaign,
        campaignName = campaignName,
        campaignImageUrl = campaignImageUrl,
        projectName = projectName,
        inviterId = inviterId,
        status = status,
        usageMode = usageMode,
        maxUses = maxUses,
        claimedCount = claimedCount,
        convertedCount = convertedCount,
        reward = reward?.toReferralRewardProgress(),
        createdAt = parseReferralInstant(createdAt) ?: return null,
        expiresAt = parseReferralInstantOrNull(expiresAt),
        updatedAt = parseReferralInstant(updatedAt) ?: return null,
    )

internal fun GetReferralResponseDto.toGetReferralResponse(json: Json): GetReferralResponse =
    GetReferralResponse(
        referralId = referralId,
        campaign = campaign,
        campaignName = campaignName,
        campaignImageUrl = campaignImageUrl,
        projectName = projectName,
        properties = properties.toReferralMatchProperties(json),
        status = status,
        expiresAt = parseReferralInstantOrNull(expiresAt),
    )

internal fun ReferralMatchResponseDto.toReferralMatch(json: Json): ReferralMatch? =
    ReferralMatch(
        referralId = referralId,
        installId = installId,
        claimMethod = claimMethod,
        claimedAt = parseReferralInstant(claimedAt) ?: return null,
        inviterId = referral.inviterId,
        campaign = referral.campaignKey,
        campaignName = referral.campaignName,
        code = referral.code,
        shortPath = referral.shortPath,
        properties = referral.properties.toReferralMatchProperties(json),
    )

internal fun ReferralDetectionDto.toReferralDetection(json: Json): ReferralDetection =
    ReferralDetection(
        referralId = referralId,
        code = code,
        campaign = campaign,
        campaignName = campaignName,
        campaignImageUrl = campaignImageUrl,
        inviterId = inviterId,
        properties = properties.toReferralMatchProperties(json),
    )

internal fun ReferralRewardProgressDto.toReferralRewardProgress(): ReferralRewardProgress =
    ReferralRewardProgress(
        configured = configured,
        status = status,
        threshold = threshold,
        remaining = remaining,
        entitlementId = entitlementId,
        duration = duration,
        grantedAt = parseReferralInstantOrNull(grantedAt),
    )

internal fun ReferralProgressResponseDto.toReferralProgress(): ReferralProgress? =
    ReferralProgress(
        referral = referral.toReferralInfo() ?: return null,
        claimedCount = claimedCount,
        convertedCount = convertedCount,
        reward = reward.toReferralRewardProgress(),
    )
