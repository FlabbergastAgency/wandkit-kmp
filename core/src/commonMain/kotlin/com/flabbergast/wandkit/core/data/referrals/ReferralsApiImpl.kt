package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.data.referrals.dto.CaptureReferralFingerprintRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.GetReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.RedeemCodeRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchResponseDto
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal fun createReferralsApi(
    httpClient: WandKitHttpClient,
    baseUrl: String,
    logger: Logger,
): WandKitApi<ReferralsApi> = WandKitApi(
    api = ReferralsApiImpl(client = httpClient.client, baseUrl = baseUrl),
    logger = logger,
)

private class ReferralsApiImpl(
    private val client: HttpClient,
    private val baseUrl: String,
) : ReferralsApi {
    override suspend fun createReferral(request: CreateReferralRequestDto): WandKitHttpResponse<CreateReferralResponseDto> {
        val response = client.post("$baseUrl/api/v1/referrals") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun getReferral(path: String): WandKitHttpResponse<GetReferralResponseDto> {
        val response = client.get("$baseUrl/api/v1/referrals/$path")
        return WandKitHttpResponse(response)
    }

    override suspend fun captureReferralFingerprint(request: CaptureReferralFingerprintRequestDto): WandKitHttpResponse<Unit> {
        val response = client.post("$baseUrl/api/v1/referrals/fingerprint") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun matchReferral(request: ReferralMatchRequestDto): WandKitHttpResponse<ReferralMatchResponseDto> {
        val response = client.post("$baseUrl/api/v1/referrals/match") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }

    override suspend fun redeemCode(request: RedeemCodeRequestDto): WandKitHttpResponse<ReferralMatchResponseDto> {
        val response = client.post("$baseUrl/api/v1/referrals/redeem") {
            setBody(request)
        }
        return WandKitHttpResponse(response)
    }
}
