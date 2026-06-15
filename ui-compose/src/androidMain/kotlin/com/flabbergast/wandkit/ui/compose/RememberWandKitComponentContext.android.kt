package com.flabbergast.wandkit.ui.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.defaultComponentContext

@Composable
internal actual fun rememberWandKitComponentContext(): ComponentContext {
    val activity = LocalActivity.current as? ComponentActivity
        ?: error("WandKitHost must be used inside a ComponentActivity")
    return retain {
        activity.defaultComponentContext()
    }
}