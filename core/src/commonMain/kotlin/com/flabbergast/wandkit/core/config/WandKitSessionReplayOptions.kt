package com.flabbergast.wandkit.core.config

/**
 * Options for the "session replay" recorder: a short, rrweb-style recording
 * of screenshot frames, touches, and [com.flabbergast.wandkit.core.WandKit.event]
 * calls kept in a ring buffer bounded by time and bytes, attached to a
 * screenshot report when the user leaves "Include a replay of the last minute"
 * on in the report card.
 *
 * Android 14+ only, and only alongside [WandKitConfig.screenshotReporting] -
 * without it a replay could never be sent, so the recorder does not start.
 * Pass `null` as [WandKitConfig.sessionReplay] (the default) and the recorder
 * never starts. The iOS targets of this library ignore it; the native WandKit
 * iOS SDK has its own recorder writing the same `wandkit-replay` v1 file.
 *
 * Nothing leaves the device unless the user sends a screenshot report with the
 * replay switch on. The buffer is discarded whenever the app goes to the
 * background, and anything left over from a previous process is deleted on the
 * next launch.
 */
public data class WandKitSessionReplayOptions(
    /**
     * How much history the ring buffer keeps, in seconds, before the oldest
     * events are evicted. Values below 1 second are clamped by the buffer.
     */
    val windowSeconds: Int = 60,
    /**
     * The buffer's byte cap - once exceeded, the oldest events are evicted
     * first. Values below 256 KB are clamped by the buffer.
     */
    val maxBytes: Int = 4 * 1024 * 1024,
    /**
     * Paints over text input content (`EditText`, and Compose text fields when
     * `ui-compose` is in use) before a frame is encoded. Password fields are
     * always masked regardless of this setting.
     */
    val maskTextInputs: Boolean = false,
    /** Paints over `WebView` content before a frame is encoded. */
    val maskWebViews: Boolean = false,
    /**
     * Paints over every piece of text (`TextView`s, Compose text) before a
     * frame is encoded - for very sensitive apps that would rather lose
     * readability than risk leaking on-screen text.
     */
    val maskAllText: Boolean = false,
    /** Whether the "Include a replay of the last minute" switch starts on. */
    val includeByDefault: Boolean = true,
    /**
     * Android only. When `true` (the default) the JPEG frames - the only heavy
     * part of a recording - are written to files in the app's private cache
     * directory instead of being held on the heap, and a frozen recording is
     * written to a file there until it is sent or dismissed. Only small
     * metadata, touches and events stay in memory. The files follow the same
     * lifetime as the in-memory buffer: deleted on eviction, on background,
     * after a report is sent or dismissed, and swept on the next launch.
     *
     * `false` keeps everything in memory, like the iOS SDK. Falls back to
     * memory anyway when the SDK was configured without a `Context`.
     */
    val persistToDisk: Boolean = true,
)
