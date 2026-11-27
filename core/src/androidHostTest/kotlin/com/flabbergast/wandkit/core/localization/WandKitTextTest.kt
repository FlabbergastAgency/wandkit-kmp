package com.flabbergast.wandkit.core.localization

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every text the SDK shows exists in every language it ships, so a new
 * screen or a new language can't go out half translated. A `WandKitText`
 * entry without an English resource already fails the build.
 */
class WandKitTextTest {
    private val res = File("src/androidMain/res")

    private fun strings(dir: File): Map<String, String> =
        Regex("""<string name="(\w+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(File(dir, "wandkit_strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2].trim() }

    @Test
    fun englishMatchesWandKitText() {
        val names = strings(File(res, "values")).keys
        assertEquals(WandKitText.entries.map { "wandkit_${it.name.lowercase()}" }.sorted(), names.sorted())
    }

    @Test
    fun everyLanguageHasEveryText() {
        val english = strings(File(res, "values"))
        val languages = res.listFiles { file -> file.name.startsWith("values-") && File(file, "wandkit_strings.xml").exists() }.orEmpty()
        assertTrue(languages.any { it.name == "values-hr" })
        for (language in languages) {
            val translated = strings(language)
            assertEquals(english.keys.sorted(), translated.keys.sorted(), "${language.name} has different texts than values")
            assertEquals(emptyList(), translated.filterValues { it.isEmpty() }.keys.toList(), "${language.name} has empty texts")
        }
    }

    @Test
    fun screensUseWandKitTextNotTypedText() {
        val typedText = Regex("""(\bText\(|\b(text|contentDescription|placeholder) = )"[^"]*[A-Za-z]""")
        val sources = listOf(File("src"), File("../ui-compose/src"))
            .flatMap { root -> root.walk().filter { it.extension == "kt" && "Main" in it.path }.toList() }
        assertTrue(sources.isNotEmpty())
        val offenders = sources.flatMap { file ->
            // Previews sit at the end of a file and may show sample text.
            val code = file.readText().substringBefore("@Preview")
            typedText.findAll(code).map { "${file.name}: ${it.value}" }.toList()
        }
        assertEquals(emptyList(), offenders, "Typed text in screens; add it to WandKitText")
    }
}
