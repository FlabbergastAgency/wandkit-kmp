package com.flabbergast.wandkit.core.components.utils

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal fun <T : Any> StateFlow<T>.toValue(scope: CoroutineScope): Value<T> = StateFlowValue(this, scope)

private class StateFlowValue<out T : Any>(
    val source: StateFlow<T>,
    private val scope: CoroutineScope,
) : Value<T>() {
    override val value: T
        get() = source.value

    override fun subscribe(observer: (T) -> Unit): Cancellation {
        val job =
            source
                .onEach { observer(it) }
                .launchIn(scope)
        return Cancellation {
            job.cancel()
        }
    }
}