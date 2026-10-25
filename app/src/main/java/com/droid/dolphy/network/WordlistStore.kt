package com.droid.dolphy.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Wordlist storage and streaming.
 *
 * Why this class exists
 * ---------------------
 * The previous implementation kept every password in a `List<String>`, saved the
 * whole list as one JSON blob inside SharedPreferences, and read imported files
 * with `readText()` + `split(Regex)`. Importing a real wordlist (rockyou is
 * ~14M lines) meant: a 140 MB String, a 14M-element list, then a multi-hundred
 * megabyte XML write into preferences — a guaranteed OOM or ANR.
 *
 * This class streams instead:
 *  - bundled packs live in `assets/wordlists/` and are read line by line,
 *  - imported lists are normalised once and written to
 *    `filesDir/wordlists/<id>.txt`,
 *  - only a tiny index (`index.json`) is kept in memory,
 *  - consumers iterate with `Sequence<String>`, so memory stays flat no matter
 *    how big the list is.
 */
class WordlistStore(private val context: Context) {

    @Serializable
    data class Entry(
        val id: String,
        val title: String,
        val count: Int,
        val minLength: Int = 8,
        val builtin: Boolean = false,
        val addedAt: Long = 0L,
    )

    @Serializable
    private data class Manifest(val version: Int = 2, val packs: List<PackRef> = emptyList())

    @Serializable
    private data class PackRef(
        val id: String,
        val file: String,
        val titleKey: String = "",
        val minLength: Int = 8,
    )

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val dir: File get() = File(context.filesDir, "wordlists").apply { mkdirs() }
    private val indexFile: File get() = File(dir, "index.json")

    // ------------------------------------------------------------------
    // Bundled packs
    // ------------------------------------------------------------------

    /** Packs shipped in the APK, in manifest order, plus the generated one. */
    fun bundledPacks(): List<Entry> {
        val fromAssets = readManifest().packs.map { ref ->
            Entry(
                id = ref.id,
                title = ref.id,
                count = countLines(assetPath(ref.file)),
                minLength = ref.minLength,
                builtin = true,
            )
        }
        return fromAssets + Entry(
            id = GENERATED_ID,
            title = GENERATED_ID,
            count = WordlistGenerator.recipes.sumOf { it.limit },
            builtin = true,
        )
    }

    /** Streams a bundled pack. */
    fun bundledSequence(id: String): Sequence<String> {
        val ref = readManifest().packs.firstOrNull { it.id == id } ?: return emptySequence()
        return readAsset(assetPath(ref.file))
    }

    // ------------------------------------------------------------------
    // Imported packs
    // ------------------------------------------------------------------

    fun importedPacks(): List<Entry> = runCatching {
        if (!indexFile.exists()) emptyList()
        else json.decodeFromString(ENTRY_LIST, indexFile.readText())
    }.getOrDefault(emptyList())

    /**
     * Imports a text file picked by the user.
     *
     * Normalises and de-duplicates while streaming, writes the result to
     * internal storage, and returns the new index entry. Returns null when the
     * file holds no usable line.
     */
    fun importFromUri(uri: Uri, maxLines: Int = MAX_IMPORT_LINES): Entry? {
        val title = displayName(uri)?.substringBeforeLast('.')?.take(48) ?: "imported"
        val id = "user_" + title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "list" } + "_" + System.currentTimeMillis().toString(36)

        val target = File(dir, "$id.txt")
        val seen = HashSet<String>(4096)
        var written = 0

        val ok = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).use { reader ->
                    target.bufferedWriter(Charsets.UTF_8).use { out ->
                        var line = reader.readLine()
                        while (line != null && written < maxLines) {
                            val candidate = normalise(line)
                            if (candidate != null && seen.add(candidate)) {
                                out.write(candidate)
                                out.write("\n")
                                written++
                            }
                            line = reader.readLine()
                        }
                    }
                }
            } != null
        }.getOrDefault(false)

        if (!ok || written == 0) {
            target.delete()
            return null
        }

        val entry = Entry(
            id = id,
            title = title,
            count = written,
            builtin = false,
            addedAt = System.currentTimeMillis(),
        )
        saveIndex(importedPacks().filterNot { it.id == id } + entry)
        return entry
    }

    fun delete(id: String) {
        File(dir, "$id.txt").delete()
        saveIndex(importedPacks().filterNot { it.id == id })
    }

    // ------------------------------------------------------------------
    // Consumption
    // ------------------------------------------------------------------

    /**
     * Streams the requested lists in order, dropping duplicates across lists and
     * anything shorter than [minLength]. Memory use is one HashSet of the
     * candidates actually tested, not the whole file.
     */
    fun combinedSequence(ids: List<String>, minLength: Int = 8): Sequence<String> {
        if (ids.isEmpty()) return emptySequence()
        val seen = HashSet<String>(4096)
        return sequence {
            for (id in ids) {
                for (password in sequenceOf(id)) {
                    if (password.length < minLength) continue
                    if (seen.add(password)) yield(password)
                }
            }
        }
    }

    /** Streams one list, bundled, generated or imported. */
    fun sequence(id: String): Sequence<String> {
        if (id == GENERATED_ID) {
            return WordlistGenerator.recipes.asSequence().flatMap { WordlistGenerator.generate(it) }
        }
        val builtin = readManifest().packs.any { it.id == id }
        return if (builtin) bundledSequence(id) else readFile(File(dir, "$id.txt"))
    }

    fun countFor(id: String): Int =
        importedPacks().firstOrNull { it.id == id }?.count
            ?: bundledPacks().firstOrNull { it.id == id }?.count
            ?: 0

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun saveIndex(entries: List<Entry>) {
        runCatching { indexFile.writeText(json.encodeToString(ENTRY_LIST, entries)) }
    }

    private fun readManifest(): Manifest = runCatching {
        context.assets.open("wordlists/manifest.json").bufferedReader().use {
            json.decodeFromString<Manifest>(it.readText())
        }
    }.getOrDefault(Manifest())

    private fun assetPath(file: String) = "wordlists/$file"

    private fun readAsset(path: String): Sequence<String> = sequence {
        val reader = runCatching { context.assets.open(path).bufferedReader(Charsets.UTF_8) }.getOrNull()
            ?: return@sequence
        try {
            var line = reader.readLine()
            while (line != null) {
                val candidate = normalise(line)
                if (candidate != null) yield(candidate)
                line = reader.readLine()
            }
        } finally {
            runCatching { reader.close() }
        }
    }

    private fun readFile(file: File): Sequence<String> = sequence {
        if (!file.exists()) return@sequence
        val reader = runCatching { file.bufferedReader(Charsets.UTF_8) }.getOrNull() ?: return@sequence
        try {
            var line = reader.readLine()
            while (line != null) {
                val candidate = normalise(line)
                if (candidate != null) yield(candidate)
                line = reader.readLine()
            }
        } finally {
            runCatching { reader.close() }
        }
    }

    private fun countLines(path: String): Int = readAsset(path).count()

    private fun displayName(uri: Uri): String? {
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        }
        return uri.lastPathSegment
    }

    companion object {
        private val ENTRY_LIST = ListSerializer(Entry.serializer())

        /** Pseudo-pack id: candidates come from [WordlistGenerator], not from a file. */
        const val GENERATED_ID = "generated"

        /** Hard cap per import: 5M lines is already ~50 MB of text. */
        const val MAX_IMPORT_LINES = 5_000_000

        /** Trims, strips a UTF-8 BOM, and rejects blank/oversized/garbage lines. */
        fun normalise(raw: String): String? {
            var value = raw.trim().removePrefix("\uFEFF")
            if (value.isEmpty()) return null
            if (value.length > 128) return null
            // Skip binary noise from mis-detected encodings.
            if (value.any { it.code < 0x20 || it.code == 0x7F }) return null
            return value
        }
    }
}
