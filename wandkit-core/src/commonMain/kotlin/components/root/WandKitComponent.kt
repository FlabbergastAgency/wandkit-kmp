package com.flabbergast.wandkit.core.components.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

public interface WandKitComponent {
    public val stack: Value<ChildStack<*, Child>>

    public fun onBackClicked()

    public sealed interface Child {
        public data object Test: Child
    }
}