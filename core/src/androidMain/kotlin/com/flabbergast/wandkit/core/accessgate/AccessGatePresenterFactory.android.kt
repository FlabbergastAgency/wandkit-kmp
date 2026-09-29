package com.flabbergast.wandkit.core.accessgate

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.accessgate.AccessGatePresenter
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.platform.PlatformContext
import com.flabbergast.wandkit.core.screenshot.CurrentActivityTracker
import java.lang.ref.WeakReference

private const val LOGGER_TAG = "[AccessGatePresenter]"

/** Lives in `ui-compose`, which `core` must not depend on - hence the name, not the class. */
internal const val ACCESS_GATE_ACTIVITY_CLASS_NAME =
    "com.flabbergast.wandkit.ui.compose.accessGate.WandKitAccessGateActivity"

internal actual fun createAccessGatePresenter(
    platformContext: PlatformContext?,
    logger: Logger,
): AccessGatePresenter? {
    val context = platformContext?.applicationContext
    if (context == null) {
        logger.error(
            LOGGER_TAG,
            "Invite gating needs a Context: call WandKit.configure(config, applicationContext) from Application.onCreate.",
        )
        return null
    }
    if (!isAccessGateActivityDeclared(context)) {
        logger.error(
            LOGGER_TAG,
            "Invite gating needs the WandKit ui-compose module (it declares $ACCESS_GATE_ACTIVITY_CLASS_NAME); " +
                "add com.flabbergast.wandkit:ui-compose to the app. Gating stays off.",
        )
        return null
    }
    AccessGateActivityPresenter.logger = logger
    return AccessGateActivityPresenter
}

private fun isAccessGateActivityDeclared(context: Context): Boolean {
    val intent = Intent().setClassName(context, ACCESS_GATE_ACTIVITY_CLASS_NAME)
    val resolved = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, 0)
        }
    }.getOrNull()
    return resolved != null
}

/**
 * Keeps the gate Activity on top of the host app while the gate is blocking.
 *
 * `WandKitHost()` overlays only render where the host mounted them, so the
 * gate is a real Activity in the host's own task instead. It is started:
 * - when the controller moves into checking/blocked and a host Activity is
 *   resumed ([present]);
 * - as soon as any host Activity is created while blocking - `configure` runs
 *   in `Application.onCreate`, so this is the very first Activity, before its
 *   first frame;
 * - whenever a host Activity resumes while blocking (the user got back to it
 *   some other way, or `configure` ran too late to see the create).
 *
 * The blocking check always reads the live controller state rather than a
 * flag set by [present]/[dismiss], so a host Activity resuming the instant the
 * gate finished cannot re-open it. Duplicate starts collapse: the intent
 * reorders an existing gate instance to the front instead of stacking a new
 * one, and a burst from the same host Activity (create, then resume) is
 * started only once. Main thread only, like every lifecycle callback.
 */
internal object AccessGateActivityPresenter : AccessGatePresenter {
    private const val DUPLICATE_START_WINDOW_MILLIS = 1_000L

    internal var logger: Logger? = null

    private var lastStartHost: WeakReference<Activity>? = null
    private var lastStartAtMillis = 0L

    private val isBlocking: Boolean
        get() = WandKitSdkContainer.activeAccessGate.value?.isBlocking == true

    override fun present() {
        CurrentActivityTracker.currentActivity?.let(::showOver)
    }

    /** The gate Activity finishes itself once it sees a non-blocking state. */
    override fun dismiss() = Unit

    internal fun onActivityCreated(activity: Activity) {
        showOver(activity)
    }

    internal fun onActivityResumed(activity: Activity) {
        showOver(activity)
    }

    internal fun isGateActivity(activity: Activity): Boolean =
        activity.javaClass.name == ACCESS_GATE_ACTIVITY_CLASS_NAME

    private fun showOver(host: Activity) {
        if (!isBlocking || isGateActivity(host) || host.isFinishing) return

        val now = SystemClock.uptimeMillis()
        if (lastStartHost?.get() === host && now - lastStartAtMillis < DUPLICATE_START_WINDOW_MILLIS) return
        lastStartHost = WeakReference(host)
        lastStartAtMillis = now

        val intent = Intent()
            .setClassName(host, ACCESS_GATE_ACTIVITY_CLASS_NAME)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        runCatching {
            host.startActivity(intent, ActivityOptions.makeCustomAnimation(host, 0, 0).toBundle())
        }.onFailure {
            logger?.error(LOGGER_TAG, "Couldn't start the invite gate screen", it)
        }
    }
}
