package com.flabbergast.wandkit.core.feedback

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The Android half of the web app's native bridge
 * (`window.WandkitAndroid.postMessage(jsonString)`, see `bridge.ts`).
 *
 * [postMessage] is invoked by the WebView on its own dedicated JavaBridge
 * thread, never the main thread - that is true of every
 * `@JavascriptInterface` method, regardless of which thread called into JS.
 * Parsing happens there, but [onMessage] is dispatched back to the main
 * thread with a [Handler], since it is expected to touch the Activity's
 * views and its main-thread coroutine scope.
 *
 * `postMessage` is deliberately left at the default (public) visibility
 * rather than `internal`: Kotlin mangles the JVM name of `internal`
 * declarations, and WebView finds this method by reflecting for the literal
 * name `postMessage` - a mangled name would silently break the bridge.
 */
internal class FeedbackBridge(
    private val json: Json,
    private val onMessage: (type: String, body: JsonObject) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(raw: String) {
        val body = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return
        val type = body["type"]?.jsonPrimitive?.contentOrNull ?: return
        mainHandler.post { onMessage(type, body) }
    }
}
