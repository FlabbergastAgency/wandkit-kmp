package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class EventPageTypeDto {
    @SerialName("thumbs")
    THUMBS,

    @SerialName("stars")
    STARS,

    @SerialName("multi_choice")
    MULTI_CHOICE,

    @SerialName("text")
    TEXT,

    @SerialName("end")
    END,
}
