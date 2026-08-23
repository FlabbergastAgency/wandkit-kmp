package com.flabbergast.wandkit.core.feedback

import com.flabbergast.wandkit.core.config.WandKitFeedbackTheme
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.posts.PostsConfig
import com.flabbergast.wandkit.core.domain.posts.PostsSession
import com.flabbergast.wandkit.core.domain.posts.PostsTag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `FeedbackBootstrap.toJavaScript` is the document-start script that seeds
 * `window.__WANDKIT__` for the hosted feedback web app. These tests pin down
 * the script's envelope, the camelCase shape of its payload (the API answers
 * in snake_case), and the two edge cases the web app depends on: an absent
 * `prefill` key rather than a `null` one, and JS-unsafe characters escaped
 * out of tag names.
 */
class FeedbackBootstrapTest {
    private val session = PostsSession(
        token = "session-token",
        expiresAt = "2026-08-21T10:00:00Z",
        readOnly = false,
        config = PostsConfig(
            enabledTypes = listOf("bug", "feature_request"),
            roadmapEnabled = true,
            tags = listOf(PostsTag(id = "t1", name = "iOS", color = "#FF0000")),
        ),
    )

    private fun bootstrap(
        theme: WandKitFeedbackTheme? = null,
        isDark: Boolean = false,
        safeAreaInsets: FeedbackBootstrap.Insets = FeedbackBootstrap.Insets(top = 44.0, bottom = 24.0, left = 0.0, right = 0.0),
        prefill: WandKitComposerPrefill? = null,
        session: PostsSession = this.session,
    ) = FeedbackBootstrap.make(
        session = session,
        theme = theme,
        isDark = isDark,
        safeAreaInsets = safeAreaInsets,
        locale = "en-US",
        platform = "android",
        prefill = prefill,
    )

    @Test
    fun scriptEnvelopeWrapsTheAssignment() {
        val script = bootstrap().toJavaScript(createJson())

        assertTrue(script.startsWith("window.__WANDKIT__ = {"), script)
        assertTrue(script.endsWith("};"), script)
    }

    @Test
    fun payloadUsesCamelCaseKeys() {
        val script = bootstrap(isDark = true).toJavaScript(createJson())

        val root = payloadOf(script)
        assertEquals("session-token", root["token"]?.jsonPrimitive?.content)
        assertEquals("2026-08-21T10:00:00Z", root["expiresAt"]?.jsonPrimitive?.content)
        assertEquals(false, root["readOnly"]?.jsonPrimitive?.boolean)

        val config = requireNotNull(root["config"]).jsonObject
        assertEquals(
            listOf("bug", "feature_request"),
            config["enabledTypes"]?.jsonArray?.map { it.jsonPrimitive.content },
        )
        assertEquals(true, config["roadmapEnabled"]?.jsonPrimitive?.boolean)

        val insets = requireNotNull(root["safeAreaInsets"]).jsonObject
        assertEquals(44.0, insets["top"]?.jsonPrimitive?.double)

        assertEquals("android", root["platform"]?.jsonPrimitive?.content)
        assertEquals("dark", root["colorScheme"]?.jsonPrimitive?.content)
    }

    @Test
    fun prefillKeyIsAbsentWhenNull() {
        val script = bootstrap(prefill = null).toJavaScript(createJson())

        assertFalse(payloadOf(script).containsKey("prefill"))
    }

    @Test
    fun prefillKeyIsAbsentWhenEmpty() {
        val script = bootstrap(prefill = WandKitComposerPrefill()).toJavaScript(createJson())

        assertFalse(payloadOf(script).containsKey("prefill"))
    }

    @Test
    fun prefillEncodesTypeAndBase64Attachment() {
        val prefill = WandKitComposerPrefill(
            title = "Crash",
            type = WandKitPostType.BUG,
            attachments = listOf(
                WandKitComposerAttachment(
                    data = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()),
                    contentType = "image/jpeg",
                    fileName = "screenshot.jpg",
                ),
            ),
        )

        val script = bootstrap(prefill = prefill).toJavaScript(createJson())
        val prefillJson = requireNotNull(payloadOf(script)["prefill"]).jsonObject

        assertEquals("bug", prefillJson["type"]?.jsonPrimitive?.content)
        assertFalse(prefillJson.containsKey("description"))

        val attachment = requireNotNull(prefillJson["attachments"]).jsonArray[0].jsonObject
        assertEquals("/9j/4A==", attachment["data"]?.jsonPrimitive?.content)
        assertEquals("image", attachment["kind"]?.jsonPrimitive?.content)
        assertEquals("image/jpeg", attachment["contentType"]?.jsonPrimitive?.content)
        assertEquals("screenshot.jpg", attachment["fileName"]?.jsonPrimitive?.content)
    }

    @Test
    fun tagNameWithLineSeparatorIsEscapedRatherThanRaw() {
        // U+2028 LINE SEPARATOR is legal inside a JSON string but illegal inside a
        // JS one: it must come out as the six-character "\u2028" escape sequence,
        // never as the raw (invisible) character.
        val sessionWithWeirdTag = session.copy(
            config = session.config.copy(
                tags = listOf(PostsTag(id = "t1", name = "Weird\u2028Tag", color = null)),
            ),
        )

        val script = bootstrap(session = sessionWithWeirdTag).toJavaScript(createJson())

        assertTrue(script.contains("\\u2028"), script)
        assertFalse(script.contains("\u2028"), script)
    }

    @Test
    fun displayNameKeyIsPresentWhenSet() {
        val script = bootstrap(session = session.copy(displayName = "Jane")).toJavaScript(createJson())

        assertEquals("Jane", payloadOf(script)["displayName"]?.jsonPrimitive?.content)
    }

    @Test
    fun displayNameKeyIsAbsentWhenNull() {
        val script = bootstrap(session = session.copy(displayName = null)).toJavaScript(createJson())

        assertFalse(payloadOf(script).containsKey("displayName"))
    }

    @Test
    fun themeKeyIsAbsentWhenNull() {
        val script = bootstrap(theme = null).toJavaScript(createJson())

        assertFalse(payloadOf(script).containsKey("theme"))
    }

    @Test
    fun themeKeyIsPresentWithPrimaryColorWhenGiven() {
        val theme = WandKitFeedbackTheme(primaryColor = "#112233")
        val script = bootstrap(theme = theme).toJavaScript(createJson())

        val themeJson = requireNotNull(payloadOf(script)["theme"]).jsonObject
        assertEquals("#112233", themeJson["primaryColor"]?.jsonPrimitive?.content)
    }

    @Test
    fun sessionRefreshJavaScriptBuildsTheCallExpression() {
        val script = FeedbackBootstrap.sessionRefreshJavaScript(createJson(), "t2", "2026-08-21T12:00:00Z")

        assertEquals(
            "window.__WANDKIT__.onSessionRefreshed && " +
                """window.__WANDKIT__.onSessionRefreshed({"token":"t2","expiresAt":"2026-08-21T12:00:00Z"});""",
            script,
        )
    }

    /** Strips the `window.__WANDKIT__ = ` / `;` envelope and parses the payload object. */
    private fun payloadOf(script: String) =
        Json.parseToJsonElement(script.removePrefix("window.__WANDKIT__ = ").removeSuffix(";")).jsonObject
}
