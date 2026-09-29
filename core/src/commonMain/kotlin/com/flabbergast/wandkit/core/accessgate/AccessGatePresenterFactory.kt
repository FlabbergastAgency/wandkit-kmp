package com.flabbergast.wandkit.core.accessgate

import com.flabbergast.wandkit.core.domain.accessgate.AccessGatePresenter
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.platform.PlatformContext

/**
 * The platform's gate screen, or `null` (after logging why) when there is none
 * - which leaves gating off.
 *
 * Android launches `WandKitAccessGateActivity` from `ui-compose` by class
 * name, so `core` never depends on it; without that module (or without a
 * `Context`) there is no screen. The iOS targets of this library have no gate
 * screen (the native WandKit iOS SDK covers iOS) and log a warning instead.
 */
internal expect fun createAccessGatePresenter(
    platformContext: PlatformContext?,
    logger: Logger,
): AccessGatePresenter?
