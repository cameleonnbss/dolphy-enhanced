package com.droid.dolphy.plugin.ai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droid.dolphy.MaterialBackground
import com.droid.dolphy.MaterialCard
import com.droid.dolphy.R
import com.droid.dolphy.SectionTopBar
import com.droid.dolphy.plugin.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AI module generator.
 *
 * Three steps, deliberately separated: configure a provider, describe the module,
 * review the generated code. Installation only happens after the code is on
 * screen — a generated plugin is executable Python with the permissions it
 * declares, so blind install would be reckless.
 */
/**
 * Endpoints that speak the OpenAI chat-completions dialect. Picking one only
 * fills the two fields — nothing is sent anywhere until Generate is pressed, and
 * the key still goes to that provider alone.
 */
private val PROVIDER_PRESETS = listOf(
    Triple("https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct:free", "OpenRouter"),
    Triple("https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", "Groq"),
    Triple("https://api.together.xyz/v1", "meta-llama/Llama-3.3-70B-Instruct-Turbo", "Together"),
    Triple("https://api.mistral.ai/v1", "mistral-large-latest", "Mistral"),
    Triple("http://localhost:11434/v1", "llama3.1", "Ollama"),
)

@Composable
fun AiModuleScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary
    val provider = remember { AiModuleProvider(context) }

    val initial = remember { provider.load() }
    var baseUrl by rememberSaveable { mutableStateOf(initial.baseUrl) }
    var model by rememberSaveable { mutableStateOf(initial.model) }
    var apiKey by rememberSaveable { mutableStateOf(initial.apiKey) }
    var prompt by rememberSaveable { mutableStateOf("") }

    var generating by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    MaterialBackground(accentColor = accent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            SectionTopBar(
                title = stringResource(R.string.ai_title),
                onBack = { navController.popBackStack() },
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.ai_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ---- provider ----
                MaterialCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accent,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.ai_provider),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PROVIDER_PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = baseUrl == preset.first && model == preset.second,
                                    onClick = {
                                        baseUrl = preset.first
                                        model = preset.second
                                    },
                                    label = { Text(preset.third) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.ai_base_url)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.ai_model)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.ai_api_key)) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                provider.save(AiModuleProvider.Config(baseUrl, apiKey, model))
                                status = context.getString(R.string.ai_saved)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ai_save))
                        }
                        Text(
                            stringResource(R.string.ai_key_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ---- prompt ----
                MaterialCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accent,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            minLines = 3,
                            label = { Text(stringResource(R.string.ai_prompt_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val config = AiModuleProvider.Config(baseUrl, apiKey, model)
                                provider.save(config)
                                generating = true
                                result = null
                                status = null
                                scope.launch {
                                    val outcome = provider.generate(prompt)
                                    generating = false
                                    outcome.onSuccess {
                                        result = it
                                        status = null
                                    }.onFailure {
                                        status = it.message
                                    }
                                }
                            },
                            enabled = !generating && prompt.isNotBlank() && apiKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Text(
                                "  " + stringResource(
                                    if (generating) R.string.ai_generating else R.string.ai_generate
                                )
                            )
                        }
                        if (generating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }

                status?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // ---- review + install ----
                result?.let { code ->
                    MaterialCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = accent,
                        contentPadding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.ai_review),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            SelectionContainer {
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val outcome = withContext(Dispatchers.IO) {
                                            runCatching {
                                                val id = Regex("__id__\\s*=\\s*\"([^\"]+)\"")
                                                    .find(code)?.groupValues?.get(1) ?: "ai_module"
                                                val file = File(context.cacheDir, "$id.dolphyplugin")
                                                file.writeText(code, Charsets.UTF_8)
                                                PluginManager.installFromFile(file)
                                                    .getOrThrow()
                                                file.delete()
                                                id
                                            }
                                        }
                                        status = outcome.fold(
                                            { context.getString(R.string.ai_installed, it) },
                                            { it.message }
                                        )
                                        if (outcome.isSuccess) result = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Text("  " + stringResource(R.string.ai_install))
                            }
                        }
                    }
                }
            }
        }
    }
}
