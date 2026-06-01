package com.flabbergast.wandkit.core.data.forms.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class SubmitFormThumbDto {
    @SerialName("up")
    UP,

    @SerialName("down")
    DOWN,
}
