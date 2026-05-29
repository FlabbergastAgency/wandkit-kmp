package com.flabbergast.wandkit.core.domain.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.jvm.JvmInline

@JvmInline
internal value class BackgroundDispatcher(
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
)
