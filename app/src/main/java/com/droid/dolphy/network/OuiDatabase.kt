package com.droid.dolphy.network

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * IEEE OUI → manufacturer lookup.
 *
 * A raw MAC address tells you almost nothing; the first three octets tell you who
 * made the radio. The bundled table in `assets/oui/manufacturers.json` is a
 * curated subset of the IEEE registry covering the vendors that actually turn up
 * in BLE and Wi-Fi scans — enough to label a scan result "Espressif" or "Apple"
 * without shipping the whole registry.
 */
object OuiDatabase {

    @Serializable
    private data class File(val version: Int = 1, val note: String = "", val ouis: Map<String, String> = emptyMap())

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var cache: Map<String, String>? = null

    private fun table(context: Context): Map<String, String> {
        cache?.let { return it }
        val loaded = runCatching {
            context.assets.open("oui/manufacturers.json").bufferedReader().use {
                json.decodeFromString<File>(it.readText()).ouis
            }
        }.getOrDefault(emptyMap())
        cache = loaded
        return loaded
    }

    fun size(context: Context): Int = table(context).size

    /** "00:03:93" for a full address, or null when the prefix is unknown. */
    fun vendor(context: Context, address: String?): String? {
        if (address.isNullOrBlank()) return null
        val prefix = address.trim().uppercase().take(8)
        if (prefix.length < 8) return null
        return table(context)[prefix]
    }

    /** True when the prefix is in the table — used to show "unknown" honestly. */
    fun isKnown(context: Context, address: String?): Boolean = vendor(context, address) != null
}
