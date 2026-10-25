package com.droid.dolphy

import java.util.Locale

/**
 * Single source of truth for every language Dolphy can display.
 *
 * Adding a language is a three-step job:
 *  1. add an entry here,
 *  2. create `app/src/main/res/values-<tag>/strings.xml` (see tools/i18n/),
 *  3. run `python tools/i18n/check_coverage.py` to see what is still missing.
 *
 * Anything not translated falls back to English, because `values/` is English.
 */
data class AppLanguage(
    /** BCP-47 tag used for resources and preferences. "system" follows the device. */
    val tag: String,
    /** Name written in the language itself, always shown in the picker. */
    val nativeName: String,
    /** English name, used for search and accessibility. */
    val englishName: String,
    /** Two-letter badge shown in the list (no emoji font dependency). */
    val badge: String,
    val rtl: Boolean = false,
)

object LanguageCatalog {

    const val SYSTEM = "system"

    val languages: List<AppLanguage> = listOf(
        AppLanguage("en", "English", "English", "EN"),
        AppLanguage("ru", "Русский", "Russian", "RU"),
        AppLanguage("fr", "Français", "French", "FR"),
        AppLanguage("es", "Español", "Spanish", "ES"),
        AppLanguage("de", "Deutsch", "German", "DE"),
        AppLanguage("it", "Italiano", "Italian", "IT"),
        AppLanguage("pt", "Português", "Portuguese", "PT"),
        AppLanguage("tr", "Türkçe", "Turkish", "TR"),
        AppLanguage("uk", "Українська", "Ukrainian", "UK"),
        AppLanguage("pl", "Polski", "Polish", "PL"),
        AppLanguage("ar", "العربية", "Arabic", "AR", rtl = true),
        AppLanguage("hi", "हिन्दी", "Hindi", "HI"),
        AppLanguage("id", "Bahasa Indonesia", "Indonesian", "ID"),
        AppLanguage("zh", "中文", "Chinese (Simplified)", "ZH"),
        AppLanguage("ja", "日本語", "Japanese", "JA"),
        AppLanguage("ko", "한국어", "Korean", "KO"),
    )

    private val byTag: Map<String, AppLanguage> =
        languages.associateBy { normalize(it.tag) }

    /** Accepts "en", "en-US", "zh_CN", "zh-rCN"… and returns the matching entry. */
    fun find(tag: String?): AppLanguage? {
        val normalized = normalize(tag)
        byTag[normalized]?.let { return it }
        val languageOnly = normalized.substringBefore('-')
        return languages.firstOrNull { normalize(it.tag) == languageOnly }
    }

    /** True when [tag] is a language we actually ship resources for. */
    fun isSupported(tag: String?): Boolean = find(tag) != null

    /** Normalises a stored or requested tag to a value we can persist. */
    fun normalizeTag(tag: String?): String {
        if (tag.isNullOrBlank() || tag.equals(SYSTEM, ignoreCase = true)) return SYSTEM
        return find(tag)?.tag ?: "en"
    }

    fun isRtl(tag: String?): Boolean = find(tag)?.rtl ?: false

    fun localeFor(tag: String?): Locale {
        val resolved = find(tag) ?: return Locale.ENGLISH
        return Locale.forLanguageTag(resolved.tag)
    }

    private fun normalize(tag: String?): String =
        tag.orEmpty()
            .trim()
            .replace('_', '-')
            .removePrefix("r")
            .lowercase(Locale.ROOT)
}
