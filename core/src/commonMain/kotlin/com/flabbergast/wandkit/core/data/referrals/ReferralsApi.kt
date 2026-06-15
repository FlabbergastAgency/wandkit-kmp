package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.data.referrals.dto.CaptureReferralFingerprintRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse
import com.flabbergast.wandkit.core.data.referrals.dto.GetReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.RedeemCodeRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchResponseDto


internal interface ReferralsApi {
    suspend fun createReferral(request: CreateReferralRequestDto): WandKitHttpResponse<CreateReferralResponseDto>
    suspend fun getReferral(path: String): WandKitHttpResponse<GetReferralResponseDto>
    suspend fun captureReferralFingerprint(request: CaptureReferralFingerprintRequestDto): WandKitHttpResponse<Unit>
    suspend fun matchReferral(request: ReferralMatchRequestDto): WandKitHttpResponse<ReferralMatchResponseDto>
    suspend fun redeemCode(request: RedeemCodeRequestDto): WandKitHttpResponse<ReferralMatchResponseDto>
}
