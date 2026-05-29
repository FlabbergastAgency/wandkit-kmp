package com.flabbergast.wandkit.ui.compose

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun WandKitViewController(): UIViewController = ComposeUIViewController {
    InternalWandKitHost()
}