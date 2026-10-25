package com.droid.dolphy.plugin.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Generates Dolphy plugin scaffolds with any OpenAI-compatible chat API.
 *
 * The endpoint is fully user-supplied: OpenRouter, Groq, Together, a local
 * llama.cpp / Ollama server with `/v1` compatibility, or anything else that
 * speaks `POST {baseUrl}/chat/completions`. Nothing is hardcoded and no request
 * is made until the user presses Generate — the app itself never phones home.
 *
 * Generated code is *shown before it can be installed*. That review step is not
 * decoration: plugins run arbitrary Python with the permissions they declare.
 */
class AiModuleProvider(private val context: Context) {

    private val prefs = context.getSharedPreferences("DolphyPrefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    data class Config(
        val baseUrl: String = DEFAULT_BASE_URL,
        val apiKey: String = "",
        val model: String = DEFAULT_MODEL,
    ) {
        val isConfigured: Boolean
            get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
    }

    fun load(): Config = Config(
        baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        model = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
    )

    fun save(config: Config) {
        prefs.edit()
            .putString(KEY_BASE_URL, config.baseUrl.trim().trimEnd('/'))
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_MODEL, config.model.trim())
            .apply()
    }

    /**
     * Asks the provider for a plugin and returns the raw file content.
     * Fences and stray prose around the code block are stripped.
     */
    suspend fun generate(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val config = load()
            require(config.isConfigured) { "Provider not configured" }
            require(prompt.isNotBlank()) { "Empty prompt" }

            val body = buildJsonObject {
                put("model", config.model)
                put("temperature", 0.3)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString()

            val endpoint = config.baseUrl.trimEnd('/') + "/chat/completions"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            }

            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

                if (code !in 200..299) {
                    error("HTTP $code: ${text.take(200)}")
                }

                val root = json.parseToJsonElement(text).jsonObject
                val content = root["choices"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                    ?: error("Provider returned no content")

                sanitize(content)
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1"
        const val DEFAULT_MODEL = "meta-llama/llama-3.3-70b-instruct"

        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_MODEL = "ai_model"

        /** Strips markdown fences and anything before the first plugin marker. */
        fun sanitize(raw: String): String {
            var text = raw.trim()
            if (text.startsWith("```")) {
                text = text.substringAfter('\n', text)
                text = text.substringBeforeLast("```", text)
            }
            val marker = text.indexOf("__id__")
            if (marker > 0) text = text.substring(marker)
            return text.trim() + "\n"
        }

        private val SYSTEM_PROMPT = """
You write plugins for Dolphy, an Android wireless-research app. Output ONE Python
file, nothing else — no explanation, no markdown fences.

Required header, at the very top:
__id__ = "snake_case_id"
__name__ = "Human Name"
__version__ = "1.0.0"
__author__ = "generated"
__description__ = "one line"
__icon__ = "rounded:extension"

Then exactly one class extending BasePlugin:

class MyPlugin(BasePlugin):
    def on_plugin_load(self):
        self.add_module(title="...", description="...", icon="extension", screen="main", section="PLUGINS")

    def screen_main(self, ui, api, state):
        return ui.column([
            ui.text("..."),
            ui.button("Run", self.run_action),
        ])

Rules:
- Only use ui.column, ui.row, ui.text, ui.button, ui.icon_button, ui.switch, ui.slider, ui.text_field, ui.progress.
- Keep state in self.<attr> and call self.refresh() after mutating it.
- Declare permissions you actually need in __permissions__ as a list, e.g.
  __permissions__ = ["wifi"] — allowed groups: wifi, bluetooth, nfc, storage, network, dex.
- No file system access outside the plugin sandbox, no subprocess, no eval.
- The plugin is for education and testing devices the user owns. Never emit code
  that disrupts third-party devices or intercepts other people's traffic.
- Keep it under 120 lines and make sure it runs: no imports outside the standard
  library, no undefined names.
"""
    }
}
