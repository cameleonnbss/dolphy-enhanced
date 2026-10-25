package com.droid.dolphy.nfc

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reference entry from `assets/nfc/cards.json`.
 *
 * This is documentation, not an exploit: it tells you what a card you just
 * scanned actually is, whether it is clonable, and why. Everything that claims a
 * card is "secure" here is a statement about the standard, not a guarantee about
 * a specific deployment.
 */
@Serializable
data class NfcCard(
    val id: String,
    val name: String,
    val category: String,
    val standard: String = "",
    val freq: String = "",
    val memory: String = "",
    val security: String = "",
    val atqa: String = "",
    val sak: String = "",
    val aid: String = "",
    val notes: String = "",
) {
    /** ATQA/SAK are the two bytes Android reports for ISO-DEP / 14443-A tags. */
    val technical: List<Pair<String, String>>
        get() = listOfNotNull(
            "Standard" to standard,
            "Frequency" to freq,
            "Memory" to memory,
            "Security" to security,
            "ATQA" to atqa,
            "SAK" to sak,
            "AID" to aid,
        ).filter { it.second.isNotBlank() }
}

@Serializable
private data class CardFile(
    val version: Int = 1,
    val note: String = "",
    val cards: List<NfcCard> = emptyList(),
)

object CardCatalog {

    private const val ASSET = "nfc/cards.json"
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var cache: List<NfcCard>? = null

    fun all(context: Context): List<NfcCard> {
        cache?.let { return it }
        val loaded = runCatching {
            context.assets.open(ASSET).bufferedReader().use {
                json.decodeFromString<CardFile>(it.readText()).cards
            }
        }.getOrDefault(emptyList())
        cache = loaded
        return loaded
    }

    fun categories(context: Context): List<String> =
        all(context).map { it.category }.distinct().sorted()

    /**
     * Matches name, category, standard, security and the technical identifiers, so
     * searching "0x08", "A0000000031010" or "transit" all work.
     */
    fun search(context: Context, query: String, category: String? = null): List<NfcCard> {
        val q = query.trim().lowercase()
        return all(context).filter { card ->
            val inCategory = category == null || card.category == category
            inCategory && (q.isEmpty() || listOf(
                card.name, card.category, card.standard, card.security,
                card.notes, card.atqa, card.sak, card.aid, card.memory,
            ).any { it.lowercase().contains(q) })
        }
    }
}
