package com.flabbergast.wandkit.core.feedback

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.flabbergast.wandkit.core.config.WandKitColorSchemePreference
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.posts.PostsSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.max

private const val TAG = "[WandKitFeedbackActivity]"
private const val REQUEST_FILE_CHOOSER = 4201
private val HEX_RGB_REGEX = Regex("^#[0-9A-Fa-f]{6}$")
private val HEX_RGBA_REGEX = Regex("^#[0-9A-Fa-f]{8}$")

/**
 * Hosts the feedback (posts) UI - the feed, the composer, a post's detail
 * screen. Everything the user sees is the WandKit-hosted web app in a
 * [WebView]; this Activity mints the session, injects it before the page's
 * own scripts run, answers the handful of bridge messages a web app cannot
 * do for itself (see [FeedbackBridge]), and shows something other than a
 * blank rectangle when the page will not load.
 *
 * Public only because the manifest has to name a public class to instantiate
 * it by reflection - open it through [com.flabbergast.wandkit.core.WandKit.presentFeedback]
 * or [feedbackIntent], not by constructing it directly; everything else here
 * is `private`.
 */
public class WandKitFeedbackActivity : Activity() {
    private enum class State { LOADING, CONTENT, FAILED }

    private lateinit var container: WandKitSdkContainer
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: View

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null
    private var refreshJob: Job? = null

    private var scriptHandler: ScriptHandler? = null
    private var pendingFallbackScript: String? = null
    private var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var backInvokedCallback: OnBackInvokedCallback? = null

    private var state: State = State.LOADING
    private var isDark: Boolean = false
    private var currentScreen: WandKitFeedbackScreen = WandKitFeedbackScreen.Feed
    private var currentSafeAreaInsets: FeedbackBootstrap.Insets = FeedbackBootstrap.Insets.Zero

    // MARK: - Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        val resolvedContainer = runCatching { WandKitSdkContainer.get() }.getOrNull()
        if (resolvedContainer == null) {
            // A process-death recreate landing here before the host has
            // called WandKit.configure() again - nothing to render.
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        container = resolvedContainer
        isDark = resolveIsDark()

        // A fixed theme, chosen before super.onCreate() and therefore before
        // the window is created, so the native chrome around the webview -
        // the spinner, the error state, the status/nav bars - matches what
        // the web app was told to render in rather than the device default.
        setTheme(
            if (isDark) android.R.style.Theme_DeviceDefault_NoActionBar
            else android.R.style.Theme_DeviceDefault_Light_NoActionBar,
        )
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark

        val root = buildContentView()
        setContentView(root)
        applyBackgroundColor()
        installInsetsListener(root)
        configureWebView()
        registerBackHandling()

        currentScreen = resolveScreen(intent)
        reload()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentScreen = resolveScreen(intent)
        reload()
    }

    override fun onStart() {
        super.onStart()
        visibleCount++
    }

    override fun onStop() {
        super.onStop()
        visibleCount = max(0, visibleCount - 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_FILE_CHOOSER) return
        val callback = pendingFileChooserCallback ?: return
        pendingFileChooserCallback = null
        callback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // On API 33+ this is normally never invoked - the callback registered
        // in registerBackHandling() intercepts back first - but it is kept as
        // the only mechanism available below 33.
        if (Build.VERSION.SDK_INT < 33) {
            goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 33) {
            backInvokedCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        }
        scope.cancel()
        scriptHandler?.remove()
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    // MARK: - Setup

    private fun resolveIsDark(): Boolean =
        when (container.config.feedbackTheme?.preferredColorScheme ?: WandKitColorSchemePreference.SYSTEM) {
            WandKitColorSchemePreference.LIGHT -> false
            WandKitColorSchemePreference.DARK -> true
            WandKitColorSchemePreference.SYSTEM -> {
                val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

    private fun resolveScreen(intent: Intent): WandKitFeedbackScreen =
        FeedbackLaunchStore.take(intent.getStringExtra(EXTRA_LAUNCH_ID)) ?: WandKitFeedbackScreen.Feed

    private fun buildContentView(): FrameLayout {
        val root = FrameLayout(this)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        root.addView(webView)

        errorView = buildErrorView()
        root.addView(errorView)

        progressBar = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
        }
        root.addView(progressBar)

        return root
    }

    private fun buildErrorView(): LinearLayout {
        val density = resources.displayMetrics.density
        val titleView = TextView(this).apply {
            text = "Couldn't load"
            setTypeface(typeface, Typeface.BOLD)
            textSize = 18f
            gravity = Gravity.CENTER
        }
        val messageView = TextView(this).apply {
            text = "Check your connection and try again."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        val retryButton = Button(this).apply {
            text = "Retry"
            setOnClickListener { reload() }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            val padding = (24 * density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(titleView)
            addView(messageView)
            addView(
                retryButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = (16 * density).toInt() },
            )
        }
    }

    private fun applyBackgroundColor() {
        val parsed = parseThemeBackgroundColor(container.config.feedbackTheme?.backgroundColor)
        when {
            parsed != null -> {
                window.setBackgroundDrawable(ColorDrawable(parsed))
                webView.setBackgroundColor(parsed)
            }
            isDark -> {
                // No theme color to fall back to: Theme_DeviceDefault
                // (dark, set above) already gives the window the right
                // background, but WebView itself defaults to white
                // regardless of the app theme, so it needs an explicit nudge.
                webView.setBackgroundColor(Color.BLACK)
            }
            // else: leave the window and the WebView on the light theme's own background.
        }
    }

    private fun parseThemeBackgroundColor(raw: String?): Int? {
        val hex = raw ?: return null
        val normalized = when {
            HEX_RGB_REGEX.matches(hex) -> hex
            // Color.parseColor expects #AARRGGBB, not #RRGGBBAA.
            HEX_RGBA_REGEX.matches(hex) -> "#" + hex.substring(7, 9) + hex.substring(1, 7)
            else -> return null
        }
        return runCatching { Color.parseColor(normalized) }.getOrNull()
    }

    private fun installInsetsListener(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val density = resources.displayMetrics.density

            currentSafeAreaInsets = FeedbackBootstrap.Insets(
                top = (bars.top / density).toDouble(),
                bottom = (bars.bottom / density).toDouble(),
                left = (bars.left / density).toDouble(),
                right = (bars.right / density).toDouble(),
            )

            (webView.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
                params.bottomMargin = max(ime.bottom - bars.bottom, 0)
                webView.layoutParams = params
            }
            errorView.setPadding(bars.left, bars.top, bars.right, bars.bottom)

            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        // Name must match the bridge.ts Android branch: window.WandkitAndroid.postMessage(...).
        webView.addJavascriptInterface(FeedbackBridge(container.json, ::onBridgeMessage), "WandkitAndroid")
        webView.webViewClient = feedbackWebViewClient
        webView.webChromeClient = feedbackWebChromeClient
    }

    private fun registerBackHandling() {
        if (Build.VERSION.SDK_INT >= 33) {
            val callback = OnBackInvokedCallback { goBack() }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            backInvokedCallback = callback
        }
    }

    private fun goBack() {
        if (webView.canGoBack()) webView.goBack() else finish()
    }

    // MARK: - Loading

    /**
     * Mints a fresh session and loads the page with it. Also what Retry
     * does - a stale or rejected session is one of the things that can put
     * the screen into the failed state, so reloading the page alone would
     * not be enough.
     */
    private fun reload() {
        loadJob?.cancel()
        apply(State.LOADING)

        val screen = currentScreen
        loadJob = scope.launch {
            container.postsSessionRepository.mintSession()
                .onSuccess { session -> load(session, screen) }
                .onFailure { error ->
                    container.logger.debug(TAG, "Posts session mint failed: ${error.message}")
                    apply(State.FAILED)
                }
        }
    }

    private fun load(session: PostsSession, screen: WandKitFeedbackScreen) {
        val prefill = (screen as? WandKitFeedbackScreen.Composer)?.prefill
        val script = FeedbackBootstrap.make(
            session = session,
            theme = container.config.feedbackTheme,
            isDark = isDark,
            safeAreaInsets = currentSafeAreaInsets,
            locale = Locale.getDefault().toLanguageTag(),
            platform = FeedbackBootstrap.PLATFORM_ANDROID,
            prefill = prefill,
        ).toJavaScript(container.json)

        // Replaced rather than left in place: Retry mints a new token, and a
        // previous attempt's script (and its stale token) is still installed.
        scriptHandler?.remove()
        scriptHandler = null
        pendingFallbackScript = null

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandler = WebViewCompat.addDocumentStartJavaScript(webView, script, setOf(webOrigin))
        } else {
            container.logger.debug(TAG, "DOCUMENT_START_SCRIPT unsupported on this WebView; falling back to onPageStarted injection")
            pendingFallbackScript = script
        }

        webView.loadUrl(initialUrl(screen))
    }

    private fun apply(newState: State) {
        state = newState
        webView.visibility = if (newState == State.CONTENT) View.VISIBLE else View.INVISIBLE
        errorView.visibility = if (newState == State.FAILED) View.VISIBLE else View.GONE
        progressBar.visibility = if (newState == State.LOADING) View.VISIBLE else View.GONE
    }

    /** The URL the webview starts on: the composer and a post detail deep-link straight to their route so the feed never flashes first. */
    private fun initialUrl(screen: WandKitFeedbackScreen): String {
        val base = container.config.feedbackWebUrl.trimEnd('/')
        return when (screen) {
            is WandKitFeedbackScreen.Feed -> base
            is WandKitFeedbackScreen.Composer -> "$base/posts/new"
            is WandKitFeedbackScreen.Post -> "$base/posts/${screen.publicId}"
        }
    }

    /** The `allowedOriginRules` entry for [WebViewCompat.addDocumentStartJavaScript]: the bootstrap carries a bearer token, so this must not be `"*"`. */
    private val webOrigin: String by lazy { originRule(container.config.feedbackWebUrl) }

    private fun originRule(url: String): String {
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: "https"
        val host = uri.host.orEmpty()
        return if (uri.port != -1) "$scheme://$host:${uri.port}" else "$scheme://$host"
    }

    /** Whether a navigation stays inside the web app; anything else is the user following a link out. */
    private fun isSameOrigin(url: Uri): Boolean {
        val target = Uri.parse(container.config.feedbackWebUrl)
        return url.scheme.equals(target.scheme, ignoreCase = true) &&
            url.host.equals(target.host, ignoreCase = true) &&
            url.port == target.port
    }

    private fun openExternally(url: Uri) {
        val opened = runCatching { startActivity(Intent(Intent.ACTION_VIEW, url)) }.isSuccess
        if (!opened) {
            container.logger.debug(TAG, "Could not open external URL: $url")
        }
    }

    // MARK: - WebViewClient / WebChromeClient

    private val feedbackWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame || isSameOrigin(request.url)) return false
            openExternally(request.url)
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            pendingFallbackScript?.let { view.evaluateJavascript(it, null) }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            apply(State.CONTENT)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // A sub-resource, or a later navigation failure after the app is
            // already up (a reload, an external cancellation), must not
            // replace a page that is working fine.
            if (request.isForMainFrame && state != State.CONTENT) {
                container.logger.debug(TAG, "Feedback webview navigation failed: ${error.description}")
                apply(State.FAILED)
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (request.isForMainFrame && state != State.CONTENT) {
                container.logger.debug(TAG, "Feedback webview http error: ${errorResponse.statusCode}")
                apply(State.FAILED)
            }
        }
    }

    private val feedbackWebChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            // A second pick before the first resolved: answer the superseded one so it never leaks.
            pendingFileChooserCallback?.onReceiveValue(null)
            pendingFileChooserCallback = filePathCallback

            val started = runCatching {
                startActivityForResult(fileChooserParams.createIntent(), REQUEST_FILE_CHOOSER)
            }.isSuccess
            if (!started) {
                pendingFileChooserCallback = null
                filePathCallback.onReceiveValue(null)
            }
            return true
        }
    }

    // MARK: - Bridge

    private fun onBridgeMessage(type: String, body: JsonObject) {
        when (type) {
            "dismiss" -> finish()
            "openExternal" -> handleOpenExternal(body)
            "refreshSession" -> handleRefreshSession()
            "haptic" -> handleHaptic(body)
            else -> container.logger.debug(TAG, "Ignoring an unknown feedback bridge message type: $type")
        }
    }

    private fun handleOpenExternal(body: JsonObject) {
        val raw = body["url"]?.jsonPrimitive?.contentOrNull ?: return
        val url = Uri.parse(raw)
        val scheme = url.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            container.logger.debug(TAG, "Refusing to open a non-http(s) external URL from the feedback bridge")
            return
        }
        openExternally(url)
    }

    /**
     * Re-mints and hands the token back to the web app, which asks for this
     * when the API rejects its token - the SDK key it would need to mint one
     * itself never leaves the native side.
     */
    private fun handleRefreshSession() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            container.postsSessionRepository.mintSession()
                .onSuccess { session ->
                    webView.evaluateJavascript(
                        FeedbackBootstrap.sessionRefreshJavaScript(container.json, session.token, session.expiresAt),
                        null,
                    )
                }
                .onFailure { error ->
                    container.logger.debug(TAG, "Posts session refresh failed: ${error.message}")
                }
        }
    }

    private fun handleHaptic(body: JsonObject) {
        val style = body["style"]?.jsonPrimitive?.contentOrNull
        val constant = when (style) {
            "medium" -> HapticFeedbackConstants.KEYBOARD_TAP
            "heavy" -> HapticFeedbackConstants.LONG_PRESS
            "success" -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            "warning", "error" -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.CONTEXT_CLICK
            // "light" and anything unrecognised.
            else -> HapticFeedbackConstants.CONTEXT_CLICK
        }
        webView.performHapticFeedback(constant)
    }

    public companion object {
        internal const val EXTRA_LAUNCH_ID: String = "com.flabbergast.wandkit.core.feedback.EXTRA_LAUNCH_ID"

        /**
         * How many instances are on screen right now. [ScreenshotDetector]
         * stays quiet while it is non-zero: a screenshot of the feedback UI
         * is not a bug report about the host app.
         */
        internal var visibleCount: Int = 0
            private set
    }
}
