package com.flabbergast.wandkit.core

import android.content.Context
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.platform.PlatformContext

public fun WandKit.configure(
    config: WandKitConfig,
    context: Context,
) {
    configureForAndroid(config, context)
}

public fun WandKit.configureForAndroid(
    config: WandKitConfig,
    context: Context,
) {
    WandKitSdkContainer.init(config, PlatformContext(context.applicationContext))
}
