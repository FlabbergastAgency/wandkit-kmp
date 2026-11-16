package com.flabbergast.wandkit.core.domain.events

internal data class IdentifyInfo(
    /** The id passed to `identify(...)`; `null` while nobody is identified - never a made-up id. */
    val userId: String?,
    val deviceId: String,
    val displayName: String? = null,
)
