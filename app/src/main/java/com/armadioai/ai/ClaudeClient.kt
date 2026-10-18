package com.armadioai.ai

import android.util.Base64
import com.armadioai.data.ClothingItem
import com.armadioai.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class ClaudeException(message: String) : Exception(message)

@Serializable
data class ItemAnalysis(
    val isClothing: Boolean = true,
    val name: String = "",
    val category: String = "altro",
    val colors: List<String> = emptyList(),
    val pattern: String = "",
    val style: String = "",
    val season: String = "tutte",
    val formality: Int = 3,
    val description: String = "",
)

@Serializable
data class OutfitIdea(
    val title: String = "",
    val itemIds: List<Long> = emptyList(),
    val why: String = "",
    val tip: String = "",
)

@Serializable
private data class OutfitIdeas(val outfits: List<OutfitIdea> = emptyList())

data class OutfitRequest(
    val items: List<ClothingItem>,
    val occasion: String,
    val notes: String,
    val weather: String?,
    val styleNotes: String,
    val anchor: ClothingItem?,
    val recentlyWorn: Set<Long>,
)

/**
 * Client per la Messages API di Anthropic.
 * Usa uno "strumento" obbligatorio (tool use) per ottenere sempre JSON strutturato e valido.
 * La chiave API è quella inserita dall'utente nelle Impostazioni.
 */
class ClaudeClient(
    private val settings: SettingsStore,
    private val http: OkHttpClient,
    private val json: Json,
) {

    /** Guarda la foto di un capo e lo descrive. */
    suspend fun analyzeItem(image: File): ItemAnalysis {
        val base64 = withContext(Dispatchers.IO) {
            Base64.encodeToString(image.readBytes(), Base64.NO_WRAP)
        }
        val content = buildJsonArray {
            addJsonObject {
                put("type", "image")
                putJsonObject("source") {
                    put("type", "base64")
                    put("media_type", "image/jpeg")
                    put("data", base64)
                }
            }
            addJsonObject {
                put("type", "text")
                put("text", "Analizza il capo nella foto e registralo con lo strumento salva_capo.")
            }
        }
        val input = callTool(Prompts.ANALYZE_SYSTEM, content, Prompts.analyzeTool, maxTokens = 1000)
        return json.decodeFromJsonElement(ItemAnalysis.serializer(), input)
    }

    /** Propone 3 outfit usando solo i capi dell'armadio. Gli id inventati vengono scartati. */
    suspend fun suggestOutfits(request: OutfitRequest): List<OutfitIdea> {
        val content = buildJsonArray {
            addJsonObject {
                put("type", "text")
                put("text", Prompts.outfitUserPrompt(request))
            }
        }
        val input = callTool(Prompts.OUTFIT_SYSTEM, content, Prompts.outfitTool, maxTokens = 3000)
        val ideas = json.decodeFromJsonElement(OutfitIdeas.serializer(), input).outfits

        val validIds = request.items.map { it.id }.toSet()
        return ideas
            .map { idea -> idea.copy(itemIds = idea.itemIds.filter { it in validIds }.distinct()) }
            .filter { it.itemIds.isNotEmpty() }
    }

    /** Richiesta minima per controllare che chiave e modello funzionino. */
    suspend fun verifyKey(apiKey: String, model: String) {
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("model", model)
                put("max_tokens", 1)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", "Ciao")
                    }
                }
            }
            post(apiKey.trim(), body)
        }
    }

    private suspend fun callTool(
        system: String,
        content: JsonArray,
        tool: JsonObject,
        maxTokens: Int,
    ): JsonObject = withContext(Dispatchers.IO) {
        val current = settings.state.value
        if (current.apiKey.isBlank()) {
            throw ClaudeException("Inserisci la tua chiave API nelle Impostazioni.")
        }
        val toolName = tool.getValue("name").jsonPrimitive.content
        val body = buildJsonObject {
            put("model", current.model)
            put("max_tokens", maxTokens)
            put("system", system)
            putJsonArray("tools") { add(tool) }
            putJsonObject("tool_choice") {
                put("type", "tool")
                put("name", toolName)
            }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", content)
                }
            }
        }
        val response = post(current.apiKey, body)
        val toolUse = response["content"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["type"]?.jsonPrimitive?.content == "tool_use" }
            ?: throw ClaudeException("Risposta dell'AI incompleta, riprova.")
        toolUse.getValue("input").jsonObject
    }

    private fun post(apiKey: String, body: JsonObject): JsonObject {
        val request = Request.Builder()
            .url(API_URL)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val (code, raw) = try {
            http.newCall(request).execute().use { r -> r.code to r.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw ClaudeException("Connessione assente o troppo lenta: controlla internet e riprova.")
        }
        if (code !in 200..299) throw ClaudeException(errorMessage(code, raw))
        return try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            throw ClaudeException("Risposta dell'AI non leggibile, riprova.")
        }
    }

    private fun errorMessage(code: Int, raw: String): String {
        val apiMessage = runCatching {
            json.parseToJsonElement(raw).jsonObject["error"]
                ?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull().orEmpty()

        return when {
            code == 401 -> "Chiave API non valida: controllala nelle Impostazioni."
            code == 403 -> "La chiave non ha accesso a questo modello."
            code == 404 -> "Modello non disponibile: scegline un altro nelle Impostazioni."
            code == 413 -> "Foto troppo grande da inviare."
            code == 429 -> "Troppe richieste ravvicinate: aspetta un attimo e riprova."
            code == 529 || code >= 500 -> "Il servizio AI è sovraccarico in questo momento, riprova tra poco."
            apiMessage.contains("credit", ignoreCase = true) ->
                "Credito API esaurito: ricaricalo su console.anthropic.com."
            apiMessage.isNotBlank() -> "Errore dell'AI: $apiMessage"
            else -> "Errore dell'AI (codice $code)."
        }
    }

    private companion object {
        const val API_URL = "https://api.anthropic.com/v1/messages"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
