package com.flabbergast.wandkit.core.replay

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * The recorder's one background thread: JPEG encoding, frame writes, and
 * every file delete. A single thread keeps those strictly ordered - a
 * session's directory is only ever deleted after the writes queued before it,
 * so a destroyed store can't leave a stray frame behind.
 */
internal object ReplayIo : Executor {
    private val executor by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "wandkit-replay").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }
        }
    }

    override fun execute(command: Runnable) {
        executor.execute {
            // A failing job must never kill the one thread every later job needs.
            runCatching { command.run() }
        }
    }
}
