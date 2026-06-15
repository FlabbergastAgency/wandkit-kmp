package com.flabbergast.wandkit.core.domain.infrastructure.concurrency

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.infrastructure.threading.BackgroundDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal interface FireAndForgetTask {
    operator fun invoke(block: suspend () -> Unit)
}

internal fun createFireAndForgetTask(
    dispatcher: BackgroundDispatcher,
    logger: Logger,
): FireAndForgetTask = FireAndForgetTaskImpl(
    dispatcher = dispatcher,
    logger = logger,
)

private const val LOGGER_TAG = "[FireAndForgetTask]"

private class FireAndForgetTaskImpl(
    private val dispatcher: BackgroundDispatcher,
    private val logger: Logger,
): FireAndForgetTask {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.debug(LOGGER_TAG, "Error occurred in fire and forget task: $throwable")
    }
    private val scope = CoroutineScope(dispatcher.dispatcher + SupervisorJob() + exceptionHandler)

    override fun invoke(block: suspend () -> Unit) {
        scope.launch { block() }
    }
}