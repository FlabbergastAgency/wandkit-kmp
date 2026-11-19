package com.flabbergast.wandkit.core.review

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.platform.PlatformContext

/**
 * Shows the OS's native app-store review prompt after a top rating on a
 * `stars` page that sets `request_native_review`, once the form has closed.
 *
 * Android runs the Play In-App Review flow: [prefetch] fetches the review
 * info as soon as the rating is given, so [request] on close only has to
 * launch it. The iOS targets of this library have no review UI (the native
 * WandKit iOS SDK covers iOS) and only log. Either way the store decides
 * whether a prompt actually appears.
 */
internal expect object NativeReviewPresenter {
    fun prefetch(platformContext: PlatformContext?, logger: Logger)

    fun request(logger: Logger)

    fun discard()
}
