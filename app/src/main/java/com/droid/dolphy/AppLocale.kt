package com.droid.dolphy

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * App-wide language handling.
 *
 * The old implementation only knew two values ("en" and "ru") and silently
 * mapped everything else to Russian, which is why every new locale looked
 * broken. It is now driven by [LanguageCatalog] and supports any tag we ship
 * resources for, plus a "follow the system" mode.
 *
 * Design notes
 * ------------
 * - The choice lives in `DolphyPrefs`, so it survives process death and behaves
 *   identically on every Android version (no AppCompat dependency needed).
 * - `values/` holds English, so a partially translated locale falls back to
 *   English instead of Russian.
 * - Layout direction is set explicitly, so RTL locales (Arabic) mirror the UI
 *   even on OEM builds that ignore `supportsRtl`.
 */
object AppLocale {

    const val PREFS = "DolphyPrefs"
    const val KEY_LANGUAGE = "app_language"

    /** Tag stored by the user, or [LanguageCatalog.SYSTEM]. */
    fun storedTag(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return LanguageCatalog.normalizeTag(prefs.getString(KEY_LANGUAGE, LanguageCatalog.SYSTEM))
    }

    /** Tag actually applied to resources: the stored one, or the device locale. */
    fun effectiveTag(context: Context): String {
        val stored = storedTag(context)
        if (stored != LanguageCatalog.SYSTEM) return stored
        val system = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0)
        } else {
            @Suppress("DEPRECATION")
            Locale.getDefault()
        }
        return LanguageCatalog.find(system.toLanguageTag())?.tag ?: "en"
    }

    /** Persists [tag] and applies it immediately. */
    fun setLanguage(context: Context, tag: String) {
        val normalized = LanguageCatalog.normalizeTag(tag)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalized)
            .apply()
        applyTo(context)
    }

    /** Recreates the activity so the new configuration is picked up. */
    fun applyTo(context: Context) {
        if (context is Activity) {
            context.recreate()
        }
    }

    /**
     * Returns a context whose resources use the selected language.
     * Call from `attachBaseContext` on every entry point (application,
     * activities, services that inflate resources).
     */
    fun wrap(context: Context): Context {
        val stored = storedTag(context)
        if (stored == LanguageCatalog.SYSTEM) {
            return context
        }
        val locale = LanguageCatalog.localeFor(stored)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}
