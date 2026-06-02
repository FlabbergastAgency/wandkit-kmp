package com.flabbergast.wandkit.sample

import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.core.config.WandKitConfig

fun initWandkit(apiKey: String, isDebugLoggingEnabled: Boolean = false) {
    WandKit.configure(WandKitConfig(apiKey, isDebugLoggingEnabled))
}
