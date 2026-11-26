package com.flabbergast.wandkit.core.domain.accessgate

import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateBlockReason
import com.flabbergast.wandkit.core.localization.WandKitStrings

/**
 * Copy the controller decides on, looked up when shown. The server's
 * `title`/`message` override the first two when non-blank. The screen's own
 * labels (placeholder, buttons) live with the view in `ui-compose`.
 */
@OptIn(InternalWandKitApi::class)
internal object AccessGateStrings {
    val TITLE: String get() = WandKitStrings.get("wandkit.access_gate.title", "Invite only")
    val MESSAGE: String get() = WandKitStrings.get("wandkit.access_gate.message", "Enter your invite code to continue.")
    val ERROR_NOT_FOUND: String get() = WandKitStrings.get("wandkit.access_gate.error.not_found", "That code isn't valid.")
    val ERROR_EXPIRED: String get() = WandKitStrings.get("wandkit.access_gate.error.expired", "That code has expired.")
    val ERROR_REVOKED: String get() = WandKitStrings.get("wandkit.access_gate.error.revoked", "That code is no longer valid.")
    val ERROR_ALREADY_USED: String get() = WandKitStrings.get("wandkit.access_gate.error.already_used", "This code has already been used.")
    val ERROR_RATE_LIMITED: String get() = WandKitStrings.get("wandkit.access_gate.error.rate_limited", "Too many attempts. Try again in a minute.")
    val ERROR_GENERIC: String get() = WandKitStrings.get("wandkit.access_gate.error.generic", "Something went wrong. Please try again.")
}

/**
 * The inline error shown above the empty code field when the gate is up
 * *because* a cached code went bad. [WandKitAccessGateBlockReason.NoCode] has
 * nothing to explain and [WandKitAccessGateBlockReason.Offline] is its own
 * screen.
 */
internal val WandKitAccessGateBlockReason.cachedPassInlineMessage: String?
    get() = when (this) {
        WandKitAccessGateBlockReason.NoCode, WandKitAccessGateBlockReason.Offline -> null
        WandKitAccessGateBlockReason.Revoked -> AccessGateStrings.ERROR_REVOKED
        WandKitAccessGateBlockReason.Expired -> AccessGateStrings.ERROR_EXPIRED
        WandKitAccessGateBlockReason.NotFound -> AccessGateStrings.ERROR_NOT_FOUND
    }
