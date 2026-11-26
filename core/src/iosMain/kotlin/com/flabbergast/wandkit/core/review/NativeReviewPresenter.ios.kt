package com.flabbergast.wandkit.core.review

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.platform.PlatformContext

private const val LOGGER_TAG = "[NativeReviewPresenter]"
private const val IOS_UNSUPPORTED =
    "The native review request is Android-only in WandKit KMP; use the native WandKit iOS SDK on iOS."

internal actual object NativeReviewPresenter {
    actual fun prefetch(platformContext: PlatformContext?, logger: Logger) {
        logger.debug(LOGGER_TAG, IOS_UNSUPPORTED)
    }

    actual fun request(logger: Logger) {
        logger.debug(LOGGER_TAG, IOS_UNSUPPORTED)
    }

    actual fun discard() = Unit
}
