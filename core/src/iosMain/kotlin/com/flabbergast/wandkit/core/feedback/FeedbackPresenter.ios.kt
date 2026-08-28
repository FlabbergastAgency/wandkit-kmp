package com.flabbergast.wandkit.core.feedback

import com.flabbergast.wandkit.core.di.WandKitSdkContainer

private const val LOGGER_TAG = "[FeedbackPresenter]"

internal actual fun presentFeedbackScreen(
    container: WandKitSdkContainer,
    screen: WandKitFeedbackScreen,
) {
    container.logger.warn(
        LOGGER_TAG,
        "presentFeedback is Android-only in WandKit KMP; use the native WandKit iOS SDK on iOS.",
    )
}
