package com.weardrop.ui.stylist

import android.app.Application
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weardrop.R
import com.weardrop.WearDropApplication
import com.weardrop.data.ClothingItem
import com.weardrop.data.ItemStatus
import com.weardrop.data.SavedOutfit
import com.weardrop.data.WearLog
import com.weardrop.data.toIdList
import com.weardrop.data.toIdString
import com.weardrop.stylist.Occasion
import com.weardrop.stylist.OutfitEngine
import com.weardrop.stylist.OutfitRequest
import com.weardrop.stylist.OutfitSuggestion
import com.weardrop.stylist.WeatherContext
import com.weardrop.weather.CityNotFoundException
import com.weardrop.weather.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

sealed interface WeatherState {
    data object Loading : WeatherState
    data object NoCity : WeatherState
    data class Ready(val weather: Weather) : WeatherState
    data class Failed(@StringRes val message: Int) : WeatherState
}

data class OutfitResult(
    val suggestion: OutfitSuggestion,
    val saved: Boolean = false,
    val worn: Boolean = false,
)

@OptIn(FlowPreview::class)
class StylistViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as WearDropApplication).container

    var occasion by mutableStateOf(Occasion.CASUAL)

    var weather by mutableStateOf<WeatherState>(WeatherState.Loading)
        private set
    var loading by mutableStateOf(false)
        private set
    /** String resource id of the message to show, if any. */
    var error by mutableStateOf<Int?>(null)
        private set
    var results by mutableStateOf<List<OutfitResult>>(emptyList())
        private set

    private val items: StateFlow<List<ClothingItem>> = container.dao.observeItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val anchor: StateFlow<ClothingItem?> = combine(container.stylistAnchor, items) { id, list ->
        list.firstOrNull { it.id == id && it.status == ItemStatus.READY }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Refresh the weather when the city changes (with a short pause while typing).
        viewModelScope.launch {
            container.settings.state
                .map { it.city.trim() }
                .distinctUntilChanged()
                .debounce(600)
                .collect { refreshWeather(force = false) }
        }
    }

    fun refreshWeather(force: Boolean = true) {
        val city = container.settings.state.value.city.trim()
        if (city.isBlank()) {
            weather = WeatherState.NoCity
            return
        }
        weather = WeatherState.Loading
        viewModelScope.launch {
            weather = try {
                WeatherState.Ready(container.weather.current(city, appLanguage(), force))
            } catch (e: CityNotFoundException) {
                WeatherState.Failed(R.string.weather_city_not_found)
            } catch (e: Exception) {
                WeatherState.Failed(R.string.weather_error)
            }
        }
    }

    fun clearAnchor() {
        container.stylistAnchor.value = null
    }

    fun generate() {
        if (loading) return
        val wardrobe = items.value
        if (!OutfitEngine.hasEnoughItems(wardrobe)) {
            error = R.string.stylist_need_items
            results = emptyList()
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val since = System.currentTimeMillis() - RECENT_DAYS * DAY_MS
                val recentlyWorn = container.dao.wearSince(since).flatMap { it.itemIds.toIdList() }.toSet()
                val currentWeather = (weather as? WeatherState.Ready)?.weather
                val request = OutfitRequest(
                    items = wardrobe,
                    occasion = occasion,
                    weather = currentWeather?.let { WeatherContext(it.temperature, it.rainy) },
                    anchor = anchor.value,
                    recentlyWorn = recentlyWorn,
                    month = LocalDate.now().monthValue,
                )
                val suggestions = withContext(Dispatchers.Default) { OutfitEngine.suggest(request) }
                results = suggestions.map { OutfitResult(it) }
                if (results.isEmpty()) error = R.string.stylist_no_results
            } finally {
                loading = false
            }
        }
    }

    fun save(index: Int) {
        val result = results.getOrNull(index) ?: return
        if (result.saved) return
        val suggestion = result.suggestion
        viewModelScope.launch {
            container.dao.insertSaved(
                SavedOutfit(
                    itemIds = suggestion.pieces.map { it.id }.toIdString(),
                    title = suggestion.title.name,
                    reasons = suggestion.reasons.joinToString(",") { it.name },
                    tip = suggestion.tip.name,
                    occasion = occasion.key,
                ),
            )
            results = results.mapIndexed { i, r -> if (i == index) r.copy(saved = true) else r }
            container.notify(R.string.outfit_saved)
        }
    }

    fun wearToday(index: Int) {
        val result = results.getOrNull(index) ?: return
        if (result.worn) return
        val suggestion = result.suggestion
        viewModelScope.launch {
            container.dao.insertWear(
                WearLog(
                    itemIds = suggestion.pieces.map { it.id }.toIdString(),
                    title = suggestion.title.name,
                ),
            )
            results = results.mapIndexed { i, r -> if (i == index) r.copy(worn = true) else r }
            container.notify(R.string.marked_worn)
        }
    }

    private fun appLanguage(): String =
        AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language

    private companion object {
        const val RECENT_DAYS = 3
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
