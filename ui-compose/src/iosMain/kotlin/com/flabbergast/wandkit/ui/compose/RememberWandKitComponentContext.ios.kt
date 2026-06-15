package com.flabbergast.wandkit.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry

@Composable
internal actual fun rememberWandKitComponentContext(): ComponentContext {
    return remember {
        DefaultComponentContext(
            lifecycle = LifecycleRegistry()
        )
    }
}