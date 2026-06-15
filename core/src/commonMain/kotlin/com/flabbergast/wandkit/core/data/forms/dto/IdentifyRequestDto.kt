package com.flabbergast.wandkit.core.data.forms.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IdentifyRequestDto(
    @SerialName("user_id")
    val userId: String,
)
