package com.flabbergast.wandkit.core.replay

/**
 * What the platform-neutral parts of the SDK need from the platform's session
 * replay recorder. Only Android has one in this library; on iOS the native
 * WandKit iOS SDK records instead, and
 * [com.flabbergast.wandkit.core.di.WandKitSdkContainer.sessionReplayRecorder]
 * stays `null`.
 */
internal interface SessionReplayRecorder {
    /** `null` when the recorder is not running. */
    val status: WandKitSessionReplayStatus?

    /** Records a `WandKit.event(...)` call. Callable from any thread. */
    fun recordEvent(name: String, properties: Map<String, String>)

    /**
     * Detaches the buffer into a snapshot, or `null` when the recorder is off
     * or has no frame yet. Main thread only; cheap - encoding happens later,
     * off the main thread, through [ReplaySnapshot.encode].
     */
    fun freeze(): ReplaySnapshot?
}
