package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.data.referrals.dto.CaptureReferralFingerprintRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.CreateReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.GetReferralResponseDto
import com.flabbergast.wandkit.core.data.referrals.dto.RedeemCodeRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchRequestDto
import com.flabbergast.wandkit.core.data.referrals.dto.ReferralMatchResponseDto


internal interface ReferralsApi {
    suspend fun createReferral(request: CreateReferralRequestDto): CreateReferralResponseDto
    suspend fun getReferral(path: String): GetReferralResponseDto
    suspend fun captureReferralFingerprint(request: CaptureReferralFingerprintRequestDto)
    suspend fun matchReferral(request: ReferralMatchRequestDto): ReferralMatchResponseDto?
    suspend fun redeemCode(request: RedeemCodeRequestDto): ReferralMatchResponseDto
}