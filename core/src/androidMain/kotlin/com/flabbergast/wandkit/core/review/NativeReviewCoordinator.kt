package com.flabbergast.wandkit.core.review

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger

private const val LOGGER_TAG = "[NativeReviewPresenter]"

/**
 * The store's review flow, as [NativeReviewCoordinator] uses it. On Android
 * this wraps Play's `ReviewManager`; tests pass a fake.
 */
internal interface ReviewFlowClient<Info : Any> {
    /** Fetches the single-use review info. [onResult] runs on the main thread. */
    fun requestReviewFlow(onResult: (Result<Info>) -> Unit)

    /** Shows the review card over the foreground screen; returns why not, or null once launched. */
    fun launchReviewFlow(info: Info): String?
}

/** Main-thread timer with a single slot. */
internal interface LaunchTimer {
    val nowMillis: Long

    /** Runs [action] after [delayMillis], replacing anything scheduled before. */
    fun schedule(delayMillis: Long, action: () -> Unit)

    fun cancel()
}

/**
 * When to fetch and when to launch the native review, free of Android so it
 * can be unit tested. [prefetch] fetches the review info as soon as the
 * rating is given; [request] on close launches it once both the info is in
 * and the form's exit animation ([exitDelayMillis]) is over, whichever comes
 * last. [discard] drops everything, including a fetch still in flight.
 *
 * Not thread-safe: every call, and every [ReviewFlowClient] callback, must
 * come in on the main thread.
 */
internal class NativeReviewCoordinator<Info : Any>(
    private val timer: LaunchTimer,
    private val exitDelayMillis: Long,
) {
    private var client: ReviewFlowClient<Info>? = null

    /** Single-use: cleared once launched, or when the form it was fetched for goes away. */
    private var reviewInfo: Info? = null
    private var isFetching = false

    /** Set by [request]: the form has closed, launch once the info and the exit animation allow. */
    private var pendingLaunch: PendingLaunch? = null

    /** Bumped on every reset, so a fetch still in flight for an old form is dropped. */
    private var generation = 0

    private class PendingLaunch(val formClosedAt: Long, val logger: Logger)

    /** [createClient] returns null when the review flow isn't available; it logs why itself. */
    fun prefetch(logger: Logger, createClient: () -> ReviewFlowClient<Info>?) {
        if (isFetching || reviewInfo != null) return
        val reviewClient = createClient() ?: return
        client = reviewClient
        isFetching = true
        val fetchGeneration = generation
        reviewClient.requestReviewFlow { result ->
            if (fetchGeneration != generation) return@requestReviewFlow
            isFetching = false
            result
                .onSuccess { info ->
                    reviewInfo = info
                    scheduleLaunch()
                }
                .onFailure { error ->
                    logger.warn(LOGGER_TAG, "Couldn't fetch the review info", error)
                    client = null
                    pendingLaunch = null
                }
        }
    }

    fun request(logger: Logger) {
        if (client == null) {
            logger.debug(LOGGER_TAG, "No review info fetched; skipping the native review")
            return
        }
        pendingLaunch = PendingLaunch(formClosedAt = timer.nowMillis, logger = logger)
        scheduleLaunch()
    }

    fun discard() = reset()

    private fun scheduleLaunch() {
        val launch = pendingLaunch ?: return
        if (reviewInfo == null) return // the fetch's completion schedules it
        val readyAt = launch.formClosedAt + exitDelayMillis
        timer.schedule(maxOf(0L, readyAt - timer.nowMillis)) { launchIfReady() }
    }

    private fun launchIfReady() {
        val launch = pendingLaunch ?: return
        val reviewClient = client ?: return
        val info = reviewInfo ?: return
        reset()

        // Play decides whether the card is shown (it is quota-limited) and
        // never reports which, so there is nothing to act on afterwards.
        val skipReason = reviewClient.launchReviewFlow(info) ?: return
        launch.logger.debug(LOGGER_TAG, "Skipping the native review: $skipReason")
    }

    private fun reset() {
        generation += 1
        timer.cancel()
        client = null
        reviewInfo = null
        isFetching = false
        pendingLaunch = null
    }
}
