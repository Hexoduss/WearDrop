package com.armadioai.ui.stylist

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armadioai.ArmadioApplication
import com.armadioai.ai.OutfitIdea
import com.armadioai.ai.OutfitRequest
import com.armadioai.data.ClothingItem
import com.armadioai.data.ItemStatus
import com.armadioai.data.SavedOutfit
import com.armadioai.data.WearLog
import com.armadioai.data.toIdList
import com.armadioai.data.toIdString
import com.armadioai.weather.Weather
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherState {
    data object Loading : WeatherState
    data object NoCity : WeatherState
    data class Ready(val weather: Weather) : WeatherState
    data class Failed(val message: String) : WeatherState
}

data class OutfitResult(
    val idea: OutfitIdea,
    val pieces: List<ClothingItem>,
    val saved: Boolean = false,
    val worn: Boolean = false,
)

val OCCASIONS = listOf(
    "Tempo libero", "Lavoro", "Università", "Aperitivo", "Cena fuori",
    "Serata", "Appuntamento", "Sport", "Cerimonia", "Viaggio",
)

@OptIn(FlowPreview::class)
class StylistViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ArmadioApplication).container

    var occasion by mutableStateOf(OCCASIONS.first())
    var notes by mutableStateOf("")

    var weather by mutableStateOf<WeatherState>(WeatherState.Loading)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var results by mutableStateOf<List<OutfitResult>>(emptyList())
        private set

    private val items: StateFlow<List<ClothingItem>> = container.dao.observeItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val readyCount: StateFlow<Int> = items
        .map { list -> list.count { it.status == ItemStatus.READY } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val anchor: StateFlow<ClothingItem?> = combine(container.stylistAnchor, items) { id, list ->
        list.firstOrNull { it.id == id && it.status == ItemStatus.READY }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Aggiorna il meteo quando cambia la città (con una piccola attesa mentre la scrivi).
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
                WeatherState.Ready(container.weather.current(city, force))
            } catch (e: Exception) {
                WeatherState.Failed(e.message ?: "Meteo non disponibile")
            }
        }
    }

    fun clearAnchor() {
        container.stylistAnchor.value = null
    }

    fun generate() {
        if (loading) return
        val wardrobe = items.value.filter { it.status == ItemStatus.READY }
        if (wardrobe.size < 3) {
            error = "Servono almeno 3 capi analizzati nell'armadio (ideale: una parte sopra, una sotto e delle scarpe)."
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val since = System.currentTimeMillis() - RECENT_DAYS * DAY_MS
                val recentlyWorn = container.dao.wearSince(since).flatMap { it.itemIds.toIdList() }.toSet()
                val settings = container.settings.state.value
                val ideas = container.claude.suggestOutfits(
                    OutfitRequest(
                        items = wardrobe,
                        occasion = occasion,
                        notes = notes,
                        weather = (weather as? WeatherState.Ready)?.weather?.forPrompt(),
                        styleNotes = settings.styleNotes,
                        anchor = anchor.value,
                        recentlyWorn = recentlyWorn,
                    ),
                )
                val byId = wardrobe.associateBy { it.id }
                results = ideas
                    .map { idea -> OutfitResult(idea, idea.itemIds.mapNotNull { byId[it] }) }
                    .filter { it.pieces.isNotEmpty() }
                if (results.isEmpty()) error = "Non sono riuscito a comporre outfit validi: riprova."
            } catch (e: Exception) {
                error = e.message ?: "Qualcosa è andato storto, riprova."
            } finally {
                loading = false
            }
        }
    }

    fun save(index: Int) {
        val result = results.getOrNull(index) ?: return
        if (result.saved) return
        viewModelScope.launch {
            container.dao.insertSaved(
                SavedOutfit(
                    title = result.idea.title,
                    itemIds = result.pieces.map { it.id }.toIdString(),
                    why = result.idea.why,
                    tip = result.idea.tip,
                    occasion = occasion,
                ),
            )
            results = results.mapIndexed { i, r -> if (i == index) r.copy(saved = true) else r }
            container.notify("Outfit salvato nei preferiti")
        }
    }

    fun wearToday(index: Int) {
        val result = results.getOrNull(index) ?: return
        if (result.worn) return
        viewModelScope.launch {
            container.dao.insertWear(
                WearLog(title = result.idea.title, itemIds = result.pieces.map { it.id }.toIdString()),
            )
            results = results.mapIndexed { i, r -> if (i == index) r.copy(worn = true) else r }
            container.notify("Segnato come indossato oggi")
        }
    }

    private companion object {
        const val RECENT_DAYS = 3
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
