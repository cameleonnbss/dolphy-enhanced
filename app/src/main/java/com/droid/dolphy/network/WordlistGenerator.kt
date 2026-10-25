package com.droid.dolphy.network

/**
 * Generates password candidates instead of shipping them.
 *
 * Why: a wordlist file is dead weight in the APK and trivially size-limited. A
 * mask produces millions of realistic candidates from a few bytes of code, which
 * is exactly what you want for WPA2 testing where the space is structured
 * (word + year, router model + suffix, keyboard runs…).
 *
 * Everything is lazy and bounded: [limit] caps how many candidates a single
 * recipe can emit, so a phone never tries to enumerate 10^8 entries.
 */
object WordlistGenerator {

    /** A named recipe shown in the UI. */
    data class Recipe(
        val id: String,
        val mask: String,
        val limit: Int = 250_000,
        val suffix: String = "",
    )

    val recipes: List<Recipe> = listOf(
        // 8-digit numeric space, walked with a stride so short runs stay useful.
        Recipe("digits8", "?d?d?d?d?d?d?d?d", 200_000),
        Recipe("years", "?w?Y", 60_000),
        Recipe("word_digits4", "?w?d?d?d?d", 120_000),
        Recipe("vendor_suffix", "?v?s", 40_000),
    )

    private val words = listOf(
        "password", "welcome", "admin", "internet", "freewifi", "home", "guest",
        "summer", "winter", "spring", "autumn", "love", "hello", "family",
        "secret", "master", "shadow", "dragon", "monkey", "football", "soccer",
        "qwerty", "azerty", "samsung", "iphone", "huawei", "xiaomi", "router",
        "wifi", "network", "office", "hotel", "school", "student", "money",
        "happy", "super", "star", "moon", "sun", "fire", "water", "earth",
    )

    private val vendors = listOf(
        "tplink", "netgear", "linksys", "dlink", "asus", "zyxel", "tenda",
        "mercusys", "totolink", "huawei", "zte", "nokia", "arris", "motorola",
        "belkin", "ubiquiti", "mikrotik", "cisco", "beeline", "mgts", "rostelecom",
    )

    private val suffixes = listOf(
        "", "1", "12", "123", "1234", "12345", "!", "!!", "2024", "2025",
        "2026", "_wifi", "-wifi", "wifi", "@123", "#1",
    )

    private val years = (1990..2035).map { it.toString() }

    /** Streams the candidates of one recipe, bounded by its own limit. */
    fun generate(recipe: Recipe): Sequence<String> = when (recipe.id) {
        "digits8" -> numeric(8, recipe.limit)
        "years" -> wordYear(recipe.limit)
        "word_digits4" -> wordDigits(recipe.limit)
        "vendor_suffix" -> vendorSuffix(recipe.limit)
        else -> emptySequence()
    }

    /**
     * All [length]-digit strings, walked with a prime stride.
     *
     * A plain 0,1,2… walk wastes the budget on 00000001-00020000, which nobody
     * uses. Striding visits the whole space evenly, so a 200k budget still
     * covers patterns that appear anywhere in the range.
     */
    private fun numeric(length: Int, limit: Int): Sequence<String> = sequence {
        val total = power10(length)
        val stride = maxOf(1L, total / maxOf(1, limit))
        var i = 0L
        var emitted = 0
        while (i < total && emitted < limit) {
            yield(i.toString().padStart(length, '0'))
            emitted++
            i += stride
        }
    }

    private fun wordYear(limit: Int): Sequence<String> = sequence {
        var emitted = 0
        for (word in words) {
            for (year in years) {
                if (emitted >= limit) return@sequence
                yield(word + year)
                emitted++
            }
            for (year in years) {
                if (emitted >= limit) return@sequence
                yield(word.replaceFirstChar { it.uppercase() } + year)
                emitted++
            }
        }
    }

    private fun wordDigits(limit: Int): Sequence<String> = sequence {
        var emitted = 0
        for (word in words) {
            for (n in 0..9999) {
                if (emitted >= limit) return@sequence
                yield(word + n.toString().padStart(4, '0'))
                emitted++
            }
        }
    }

    private fun vendorSuffix(limit: Int): Sequence<String> = sequence {
        var emitted = 0
        for (vendor in vendors) {
            for (suffix in suffixes) {
                if (emitted >= limit) return@sequence
                yield(vendor + suffix)
                emitted++
            }
        }
    }

    private fun power10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }
}
