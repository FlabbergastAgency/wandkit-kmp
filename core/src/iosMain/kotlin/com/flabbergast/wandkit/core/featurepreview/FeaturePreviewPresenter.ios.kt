package com.flabbergast.wandkit.core.featurepreview

import com.flabbergast.wandkit.core.di.WandKitSdkContainer

private const val LOGGER_TAG = "[FeaturePreviewPresenter]"

internal actual fun presentFeaturePreviewFlow(
    container: WandKitSdkContainer,
    postId: String,
    onResult: (WandKitFeaturePreviewResult) -> Unit,
) {
    container.logger.warn(
        LOGGER_TAG,
        "presentFeaturePreview is Android-only in WandKit KMP; use the native WandKit iOS SDK on iOS.",
    )
}
