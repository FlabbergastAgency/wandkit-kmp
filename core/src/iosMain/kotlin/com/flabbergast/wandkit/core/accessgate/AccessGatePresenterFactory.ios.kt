package com.flabbergast.wandkit.core.accessgate

import com.flabbergast.wandkit.core.domain.accessgate.AccessGatePresenter
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.platform.PlatformContext

private const val LOGGER_TAG = "[AccessGatePresenter]"

internal actual fun createAccessGatePresenter(
    platformContext: PlatformContext?,
    logger: Logger,
): AccessGatePresenter? {
    logger.warn(
        LOGGER_TAG,
        "Invite gating is Android-only in WandKit KMP; use the native WandKit iOS SDK on iOS. Gating stays off.",
    )
    return null
}
