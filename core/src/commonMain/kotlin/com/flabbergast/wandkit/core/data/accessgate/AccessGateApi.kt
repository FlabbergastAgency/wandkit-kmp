package com.flabbergast.wandkit.core.data.accessgate

import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimResponseDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitHttpResponse

internal interface AccessGateApi {
    suspend fun status(request: AccessGateStatusRequestDto): WandKitHttpResponse<AccessGateStatusResponseDto>
    suspend fun claim(request: AccessGateClaimRequestDto): WandKitHttpResponse<AccessGateClaimResponseDto>
}
