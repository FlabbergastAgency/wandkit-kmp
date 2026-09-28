package com.flabbergast.wandkit.core.replay

/**
 * A snapshot of the session replay recorder, for host-app indicators (a
 * "recording" dot in a debug menu, say). See
 * [com.flabbergast.wandkit.core.WandKit.sessionReplayStatus].
 */
public data class WandKitSessionReplayStatus(
    /** JPEG frames currently held by the ring buffer. */
    val frameCount: Int,
    /** Approximate bytes held by the ring buffer (on disk or in memory). */
    val bufferedBytes: Int,
    /** `true` while the app is in the background, SDK UI is up, or capture keeps coming back blank. */
    val isPaused: Boolean,
    /** `true` when frames are being written to disk rather than held in memory. */
    val isPersistedToDisk: Boolean,
)
