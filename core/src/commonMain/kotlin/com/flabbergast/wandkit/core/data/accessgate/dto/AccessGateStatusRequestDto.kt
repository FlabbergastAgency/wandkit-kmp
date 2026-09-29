package com.flabbergast.wandkit.core.data.accessgate.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST /api/v1/sdk/access-gate/status` - the silent launch-time check. Writes
 * nothing server-side.
 */
@Serializable
internal data class AccessGateStatusRequestDto(
    @SerialName("install_id")
    val installId: String,
    /**
     * The cached pass's code. `null` is omitted from the body (the SDK's
     * `Json` has `explicitNulls = false`) when there is no pass to re-check.
     */
    @SerialName("code")
    val code: String? = null,
)
