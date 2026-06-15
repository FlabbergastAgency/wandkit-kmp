package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.RedeemCodeRequestDto
import com.flabbergast.wandkit.core.data.referrals.mappers.toGetReferralResponse
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralInfo
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralMatch
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralRequestProperties
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.referrals.ReferralsRepository
import com.flabbergast.wandkit.core.platform.InstallReferralCodeProvider
import com.flabbergast.wandkit.core.domain.referrals.GetReferralResponse
import com.flabbergast.wandkit.core.domain.referrals.ReferralInfo
import com.flabbergast.wandkit.core.domain.referrals.ReferralMatch
import kotlinx.serialization.json.Json

internal fun createReferralsRepository(
    referralsApi: WandKitApi<ReferralsApi>,
    installReferralCodeProvider: InstallReferralCodeProvider,
    deviceId: String,
    json: Json,
    logger: Logger,
): ReferralsRepository = ReferralsRepositoryImpl(
    referralsApi = referralsApi,
    installReferralCodeProvider = installReferralCodeProvider,
    deviceId = deviceId,
    json = json,
    logger = logger,
)

private const val LOGGER_TAG = "[ReferralsRepository]"

private class ReferralsRepositoryImpl(
    private val referralsApi: WandKitApi<ReferralsApi>,
    private val installReferralCodeProvider: InstallReferralCodeProvider,
    private val deviceId: String,
    private val json: Json,
    private val logger: Logger,
) : ReferralsRepository {
    override suspend fun invite(
        userId: String,
        campaign: String,
        properties: Map<String, String>,
    ): ReferralInfo? =
        referralsApi {
            createReferral(
                CreateReferralRequestDto(
                    campaignKey = campaign,
                    userId = userId,
                    properties = properties.toReferralRequestProperties(),
                )
            )
        }
            .onSuccess {
                logger.debug(
                    LOGGER_TAG,
                    "Invited referral for userId: $userId, campaign: $campaign"
                )
            }.onFailure {
                logger.warn(
                    LOGGER_TAG,
                    "Couldn't invite referral for userId: $userId, campaign: $campaign",
                    it
                )
            }.map {
                it.data.toReferralInfo()
            }.getOrNull()

    override suspend fun getReferral(path: String): GetReferralResponse? =
        referralsApi {
            getReferral(path)
        }
            .onSuccess {
                logger.debug(LOGGER_TAG, "Fetched referral for path: $path")
            }.onFailure {
                logger.warn(LOGGER_TAG, "Couldn't fetch referral for path: $path", it)
            }.map {
                it.data
                    .toGetReferralResponse(json)
            }.getOrNull()

    override suspend fun matchReferral(): ReferralMatch? {
        val code =
            installReferralCodeProvider.getReferralCode()?.trim().takeUnless { it.isNullOrEmpty() }
        if (code == null) {
            logger.debug(LOGGER_TAG, "No install referral code available.")
            return null
        }

        return redeemCode(code)
    }

    override suspend fun redeemCode(code: String): ReferralMatch? =
        referralsApi {
            redeemCode(
                RedeemCodeRequestDto(
                    installId = deviceId,
                    code = code,
                )
            )
        }
            .onSuccess {
                logger.debug(LOGGER_TAG, "Redeemed referral code.")
            }.onFailure {
                logger.warn(LOGGER_TAG, "Couldn't redeem referral code.", it)
            }.map {
                it.data.toReferralMatch(json)
            }.getOrNull()
}