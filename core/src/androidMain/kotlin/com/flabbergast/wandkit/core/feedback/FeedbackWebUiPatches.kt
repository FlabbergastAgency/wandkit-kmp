package com.flabbergast.wandkit.core.feedback

/**
 * CSS patches for known layout bugs in the hosted feedback web app.
 *
 * The compose page's attachment thumbnail wraps the preview and remove button
 * in one `overflow-hidden` box, but the remove badge is positioned outside it
 * (`-top-1.5 -right-1.5`), so the X gets clipped. Keep rounded corners on the
 * media itself and let the badge sit outside the clip rect.
 */
internal object FeedbackWebUiPatches {
    private const val STYLE_ID = "wandkit-ui-patches"

    private val css: String =
        """
        .flex.flex-wrap.gap-2 > div.relative.overflow-hidden[class*="h-[72px]"] {
          overflow: visible !important;
        }
        .flex.flex-wrap.gap-2 > div.relative.overflow-hidden[class*="h-[72px]"] > img,
        .flex.flex-wrap.gap-2 > div.relative.overflow-hidden[class*="h-[72px]"] > video {
          border-radius: 10px;
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        """.trimIndent()

    fun documentStartScript(): String =
        """
        (function() {
            var css = ${escapeForJavaScript(css)};
            var inject = function() {
                if (document.getElementById('$STYLE_ID')) return;
                var style = document.createElement('style');
                style.id = '$STYLE_ID';
                style.textContent = css;
                (document.head || document.documentElement).appendChild(style);
            };
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', inject);
            } else {
                inject();
            }
        })();
        """.trimIndent()

    private fun escapeForJavaScript(text: String): String =
        buildString {
            append('"')
            text.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\u2028' -> append("\\u2028")
                    '\u2029' -> append("\\u2029")
                    else -> append(char)
                }
            }
            append('"')
        }
}
