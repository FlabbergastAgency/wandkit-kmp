package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpException
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.RedeemCodeRequestDto
import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.referrals.mappers.toGetReferralResponse
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralDetection
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralProgress
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralInfo
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralMatch
import com.flabbergast.wandkit.core.data.referrals.mappers.toReferralRequestProperties
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.install.InstallIdentity
import com.flabbergast.wandkit.core.domain.referrals.ReferralsRepository
import com.flabbergast.wandkit.core.platform.InstallReferralCodeProvider
import com.flabbergast.wandkit.core.platform.readDeviceFingerprint
import com.flabbergast.wandkit.core.domain.referrals.GetReferralResponse
import com.flabbergast.wandkit.core.domain.referrals.ReferralDetection
import com.flabbergast.wandkit.core.domain.referrals.ReferralInfo
import com.flabbergast.wandkit.core.domain.referrals.ReferralMatch
import com.flabbergast.wandkit.core.domain.referrals.ReferralProgress
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal fun createReferralsRepository(
    referralsApi: WandKitApi<ReferralsApi>,
    installReferralCodeProvider: InstallReferralCodeProvider,
    installIdentity: InstallIdentity,
    detectionStore: ReferralDetectionStore,
    appConfiguration: AppConfiguration,
    json: Json,
    logger: Logger,
): ReferralsRepository = ReferralsRepositoryImpl(
    referralsApi = referralsApi,
    installReferralCodeProvider = installReferralCodeProvider,
    installIdentity = installIdentity,
    detectionStore = detectionStore,
    appConfiguration = appConfiguration,
    json = json,
    logger = logger,
)

private const val LOGGER_TAG = "[ReferralsRepository]"

private class ReferralsRepositoryImpl(
    private val referralsApi: WandKitApi<ReferralsApi>,
    private val installReferralCodeProvider: InstallReferralCodeProvider,
    private val installIdentity: InstallIdentity,
    private val detectionStore: ReferralDetectionStore,
    private val appConfiguration: AppConfiguration,
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

    override suspend fun getReferralProgress(
        userId: String,
        campaign: String,
    ): ReferralProgress? =
        referralsApi.optional(HttpStatusCode.NotFound) {
            getReferralProgress(userId = userId, campaign = campaign)
        }
            .onSuccess {
                logger.debug(LOGGER_TAG, "Fetched referral progress for userId: $userId, campaign: $campaign")
            }.onFailure {
                logger.warn(
                    LOGGER_TAG,
                    "Couldn't fetch referral progress for userId: $userId, campaign: $campaign",
                    it,
                )
            }.getOrNull()
            ?.data
            ?.toReferralProgress()

    override val detectedReferral: ReferralDetection?
        get() = detectionStore.detection

    override suspend fun detectReferral(): ReferralDetection? = requestDetection().getOrNull()

    override suspend fun detectReferralOnFirstLaunchIfNeeded() {
        if (detectionStore.detectionAttempted) return

        requestDetection().onFailure { error ->
            // A transient failure is worth another launch - a dropped attempt
            // costs an inviter a referral they earned. A permanent one is not:
            // a rejected key or an unreadable body fails the same way every
            // time, and retrying just repeats the fingerprint POST for the life
            // of the install. The ceiling bounds even the transient case.
            if (isTransientDetectionFailure(error) && detectionStore.detectionFailureCount + 1 < MAX_DETECTION_RETRIES) {
                detectionStore.recordDetectionFailure()
                logger.debug(LOGGER_TAG, "Referral detection failed, retrying next launch.")
            } else {
                detectionStore.markDetectionAttempted()
                logger.debug(LOGGER_TAG, "Referral detection gave up.")
            }
        }
    }

    /**
     * Separates "the server says no referral" from "we never got an answer",
     * which [detectReferral] flattens to null but first-launch detection has to
     * tell apart.
     *
     * Records the attempt on any definitive answer, so a manual detect also
     * settles the question first-launch detection would otherwise re-ask.
     */
    private suspend fun requestDetection(): Result<ReferralDetection?> =
        referralsApi.optional(HttpStatusCode.NoContent) {
            detectReferral(
                createDetectReferralRequest(
                    installId = installIdentity.installId,
                    firstLaunchAt = installIdentity.firstLaunchAt,
                    appConfiguration = appConfiguration,
                    fingerprint = readDeviceFingerprint(),
                )
            )
        }
            .onFailure {
                logger.warn(LOGGER_TAG, "Couldn't detect referral.", it)
            }.map { success ->
                val detection = success?.data?.toReferralDetection(json)
                if (detection == null) {
                    logger.debug(LOGGER_TAG, "No referral detected for this install.")
                } else {
                    logger.debug(LOGGER_TAG, "Detected referral for this install.")
                    detectionStore.setDetection(detection)
                }
                detectionStore.markDetectionAttempted()
                detection
            }

    override fun clearDetectedReferral() {
        detectionStore.clearDetection()
    }

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
                    installId = installIdentity.installId,
                    code = code,
                )
            )
        }
            .onSuccess {
                logger.debug(LOGGER_TAG, "Redeemed referral code.")
                detectionStore.clearDetection()
            }.onFailure {
                logger.warn(LOGGER_TAG, "Couldn't redeem referral code.", it)
            }.map {
                it.data.toReferralMatch(json)
            }.getOrNull()
}

/** How many transient failures detection retries across launches before it stops asking. */
private const val MAX_DETECTION_RETRIES = 5

/**
 * Whether a later launch could plausibly get a different answer.
 *
 * 404 counts as transient: detect answers "no referral" with 204, so a 404 is by
 * construction an endpoint that is not there - deploy skew or a wrong baseURL -
 * rather than the server's answer.
 */
internal fun isTransientDetectionFailure(error: Throwable): Boolean =
    when (error) {
        is WandKitHttpException ->
            error.statusCode == 404 ||
                error.statusCode == 408 ||
                error.statusCode == 429 ||
                error.statusCode in 500..599
        is SerializationException -> false
        else -> true
    }
