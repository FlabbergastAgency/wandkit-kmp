package com.flabbergast.wandkit.core.data.referrals.mappers

import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.GetReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchResponseDto
import com.flabbergast.wandkit.core.domain.referrals.GetReferralResponse
import com.flabbergast.wandkit.core.domain.referrals.ReferralInfo
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
