package com.flabbergast.wandkit.core.domain.events

internal data class IdentifyInfo(
    /** The id passed to `identify(...)`; `null` while nobody is identified - never a made-up id. */
    val userId: String?,
    /** The current anonymous id; it changes on logout and account switch, see `InstallIdentity.deviceId`. */
    val deviceId: String,
    val displayName: String? = null,
)
