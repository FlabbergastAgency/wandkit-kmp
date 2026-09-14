package com.flabbergast.wandkit.core.featurepreview

import com.flabbergast.wandkit.core.di.WandKitSdkContainer

/**
 * Resolves [postId]'s feature-preview state and shows the sheet.
 *
 * Android fetches the state in the background, publishes the resolved
 * prompt to [WandKitSdkContainer.featurePreviewController] so `WandKitHost()`
 * renders it, and records `feature_preview_opened`; the iOS targets of this
 * library have no feature-preview UI (the native WandKit iOS SDK covers iOS)
 * and log a warning instead.
 */
internal expect fun presentFeaturePreviewFlow(
    container: WandKitSdkContainer,
    postId: String,
    onResult: (WandKitFeaturePreviewResult) -> Unit,
)
