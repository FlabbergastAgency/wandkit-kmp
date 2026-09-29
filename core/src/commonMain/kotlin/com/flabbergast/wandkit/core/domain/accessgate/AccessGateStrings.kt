package com.flabbergast.wandkit.core.domain.accessgate

import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateBlockReason

/**
 * Copy the controller decides on. Hard-coded English like the rest of the KMP
 * SDK; the server's `title`/`message` override the first two when non-blank.
 * The screen's own labels (placeholder, buttons) live with the view in
 * `ui-compose`.
 */
internal object AccessGateStrings {
    const val TITLE = "Invite only"
    const val MESSAGE = "Enter your invite code to continue."
    const val ERROR_NOT_FOUND = "That code isn't valid."
    const val ERROR_EXPIRED = "That code has expired."
    const val ERROR_REVOKED = "That code is no longer valid."
    const val ERROR_RATE_LIMITED = "Too many attempts. Try again in a minute."
    const val ERROR_GENERIC = "Something went wrong. Please try again."
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
