package com.flabbergast.wandkit.core.feedback

import com.flabbergast.wandkit.core.config.WandKitFeedbackTheme
import com.flabbergast.wandkit.core.domain.posts.PostsSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/**
 * The `window.__WANDKIT__` object, injected at document start so it is already
 * there when the web app's own scripts run.
 *
 * Encoded with the SDK's plain [Json], which means every key the web app sees
 * is the camelCase property name written here - `config` included, even
 * though it arrived from the API in snake_case.
 */
@Serializable
internal data class FeedbackBootstrap(
    val token: String,
    val expiresAt: String,
    val readOnly: Boolean,
    val config: Config,
    /** `null` when the host configured no feedback theme: the web app keeps its defaults. */
    val theme: Theme? = null,
    /** Always `light` or `dark`: `SYSTEM` is resolved natively. */
    val colorScheme: String,
    /** In CSS px (== dp). */
    val safeAreaInsets: Insets,
    val locale: String,
    val platform: String,
    /** Present only when the host opened the composer with seed content. The web app consumes it once. */
    val prefill: Prefill? = null,
    /** The user's effective display name - their own if set, else the host's suggestion. `null` for an anonymous session. */
    val displayName: String? = null,
) {
    @Serializable
    internal data class Config(
        val enabledTypes: List<String>,
        val roadmapEnabled: Boolean,
        val tags: List<Tag>,
    )

    @Serializable
    internal data class Tag(
        val id: String,
        val name: String,
        val color: String? = null,
    )

    @Serializable
    internal data class Theme(
        val primaryColor: String? = null,
        val backgroundColor: String? = null,
        val cornerRadius: Double? = null,
        val fontFamily: String? = null,
    )

    @Serializable
    internal data class Insets(
        val top: Double,
        val bottom: Double,
        val left: Double,
        val right: Double,
    ) {
        internal companion object {
            val Zero = Insets(0.0, 0.0, 0.0, 0.0)
        }
    }

    @Serializable
    internal data class Prefill(
        val title: String? = null,
        val description: String? = null,
        /** The type's wire value, e.g. `bug`. */
        val type: String? = null,
        val attachments: List<Attachment>? = null,
    )

    @Serializable
    internal data class Attachment(
        val kind: String,
        val contentType: String,
        val fileName: String,
        /** Standard base64 (no `data:` prefix), which `atob` decodes as is. */
        val data: String,
    )

    /** The document-start script that defines the object. */
    fun toJavaScript(json: Json): String =
        "window.__WANDKIT__ = ${escapeForJavaScript(json.encodeToString(this))};"

    internal companion object {
        const val PLATFORM_ANDROID = "android"

        fun make(
            session: PostsSession,
            theme: WandKitFeedbackTheme?,
            isDark: Boolean,
            safeAreaInsets: Insets,
            locale: String,
            platform: String,
            prefill: WandKitComposerPrefill?,
        ): FeedbackBootstrap = FeedbackBootstrap(
            token = session.token,
            expiresAt = session.expiresAt,
            readOnly = session.readOnly,
            config = Config(
                enabledTypes = session.config.enabledTypes,
                roadmapEnabled = session.config.roadmapEnabled,
                tags = session.config.tags.map { Tag(id = it.id, name = it.name, color = it.color) },
            ),
            theme = theme?.let {
                Theme(
                    primaryColor = it.primaryColor,
                    backgroundColor = it.backgroundColor,
                    cornerRadius = it.cornerRadius,
                    fontFamily = it.fontFamily,
                )
            },
            colorScheme = if (isDark) "dark" else "light",
            safeAreaInsets = safeAreaInsets,
            locale = locale,
            platform = platform,
            prefill = prefill?.takeUnless { it.isEmpty }?.toPayload(),
            displayName = session.displayName,
        )

        /** What `refreshSession` hands back, as a JS call the webview can evaluate. */
        fun sessionRefreshJavaScript(json: Json, token: String, expiresAt: String): String {
            val payload = escapeForJavaScript(json.encodeToString(RefreshedSession(token, expiresAt)))
            return "window.__WANDKIT__.onSessionRefreshed && window.__WANDKIT__.onSessionRefreshed($payload);"
        }

        /**
         * JSON is not quite a subset of JavaScript: U+2028 and U+2029 are legal
         * inside a JSON string and illegal inside a JS one, so a tag name
         * carrying either would otherwise turn the injected script into a
         * syntax error.
         */
        fun escapeForJavaScript(jsonText: String): String =
            jsonText
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029")
    }

    @Serializable
    private data class RefreshedSession(
        val token: String,
        val expiresAt: String,
    )
}

/** The wire shape of a prefill: raw type value, base64 bytes, no `attachments` key when there are none. */
internal fun WandKitComposerPrefill.toPayload(): FeedbackBootstrap.Prefill = FeedbackBootstrap.Prefill(
    title = title,
    description = description,
    type = type?.wireValue,
    attachments = attachments.takeIf { it.isNotEmpty() }?.map { attachment ->
        FeedbackBootstrap.Attachment(
            kind = attachment.kind,
            contentType = attachment.contentType,
            fileName = attachment.fileName,
            data = Base64.encode(attachment.data),
        )
    },
)
