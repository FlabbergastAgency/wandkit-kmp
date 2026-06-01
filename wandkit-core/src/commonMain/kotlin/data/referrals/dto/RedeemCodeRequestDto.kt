package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RedeemCodeRequestDto(
    @SerialName("install_id")
    val installId: String,
    @SerialName("code")
    val code: String,
)
