package com.flabbergast.wandkit.core.components

import com.flabbergast.wandkit.core.di.WandKitSdkContainer

internal interface ComponentFactory {
    val container: WandKitSdkContainer
}