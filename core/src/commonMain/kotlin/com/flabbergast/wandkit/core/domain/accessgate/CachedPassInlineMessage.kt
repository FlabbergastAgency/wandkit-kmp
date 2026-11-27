package com.flabbergast.wandkit.core.domain.accessgate

import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateBlockReason
import com.flabbergast.wandkit.core.localization.WandKitText

/**
 * The inline error shown above the empty code field when the gate is up
 * *because* a cached code went bad. [WandKitAccessGateBlockReason.NoCode] has
 * nothing to explain and [WandKitAccessGateBlockReason.Offline] is its own
 * screen.
 */
internal val WandKitAccessGateBlockReason.cachedPassInlineMessage: String?
    get() = when (this) {
        WandKitAccessGateBlockReason.NoCode, WandKitAccessGateBlockReason.Offline -> null
        WandKitAccessGateBlockReason.Revoked -> WandKitText.ACCESS_GATE_ERROR_REVOKED.string
        WandKitAccessGateBlockReason.Expired -> WandKitText.ACCESS_GATE_ERROR_EXPIRED.string
        WandKitAccessGateBlockReason.NotFound -> WandKitText.ACCESS_GATE_ERROR_NOT_FOUND.string
    }
