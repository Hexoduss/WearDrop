package com.weardrop.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val MAX_PREFERRED_STYLES = 3

data class AppSettings(
    val city: String = "",
    /** Up to [MAX_PREFERRED_STYLES] favorite styles, most important first. */
    val preferredStyles: List<Style> = emptyList(),
)

/**
 * Settings stored on this device only.
 * The language is handled separately by AppCompat (per-app locales).
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        AppSettings(
            city = prefs.getString(KEY_CITY, "").orEmpty(),
            preferredStyles = Style.parseList(prefs.getString(KEY_STYLES, "").orEmpty()),
        ),
    )
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        prefs.edit()
            .putString(KEY_CITY, next.city)
            .putString(KEY_STYLES, next.preferredStyles.joinToString(",") { it.key })
            .apply()
        _state.value = next
    }

    private companion object {
        const val KEY_CITY = "city"
        const val KEY_STYLES = "preferred_styles"
    }
}
