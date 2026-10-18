package com.armadioai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ModelOption(val id: String, val label: String, val description: String)

val MODEL_OPTIONS = listOf(
    ModelOption("claude-haiku-5-5", "Haiku", "Il più veloce ed economico"),
    ModelOption("claude-sonnet-5-5", "Sonnet", "Equilibrio tra qualità e costo (consigliato)"),
    ModelOption("claude-opus-5-5", "Opus", "Massima qualità, più lento e costoso"),
)

const val DEFAULT_MODEL = "claude-sonnet-5-5"

data class AppSettings(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val city: String = "",
    val styleNotes: String = "",
)

/** Impostazioni salvate solo su questo telefono. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        AppSettings(
            apiKey = prefs.getString(KEY_API, "").orEmpty(),
            model = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
            city = prefs.getString(KEY_CITY, "").orEmpty(),
            styleNotes = prefs.getString(KEY_STYLE, "").orEmpty(),
        ),
    )
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        prefs.edit()
            .putString(KEY_API, next.apiKey)
            .putString(KEY_MODEL, next.model)
            .putString(KEY_CITY, next.city)
            .putString(KEY_STYLE, next.styleNotes)
            .apply()
        _state.value = next
    }

    private companion object {
        const val KEY_API = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_CITY = "city"
        const val KEY_STYLE = "style_notes"
    }
}
