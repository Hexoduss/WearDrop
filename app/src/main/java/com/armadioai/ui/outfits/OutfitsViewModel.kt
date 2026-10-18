package com.armadioai.ui.outfits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armadioai.ArmadioApplication
import com.armadioai.data.ClothingItem
import com.armadioai.data.SavedOutfit
import com.armadioai.data.WearLog
import com.armadioai.data.toIdList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedEntry(val outfit: SavedOutfit, val pieces: List<ClothingItem>)
data class HistoryEntry(val log: WearLog, val pieces: List<ClothingItem>)

class OutfitsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ArmadioApplication).container
    private val itemsById = container.dao.observeItems().map { list -> list.associateBy { it.id } }

    val saved: StateFlow<List<SavedEntry>?> =
        combine(container.dao.observeSaved(), itemsById) { outfits, items ->
            outfits.map { outfit -> SavedEntry(outfit, outfit.itemIds.toIdList().mapNotNull { items[it] }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<HistoryEntry>?> =
        combine(container.dao.observeWearLog(), itemsById) { logs, items ->
            logs.map { log -> HistoryEntry(log, log.itemIds.toIdList().mapNotNull { items[it] }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteSaved(outfit: SavedOutfit) {
        viewModelScope.launch { container.dao.deleteSaved(outfit) }
    }

    fun wearToday(entry: SavedEntry) {
        if (entry.pieces.isEmpty()) return
        viewModelScope.launch {
            container.dao.insertWear(WearLog(title = entry.outfit.title, itemIds = entry.outfit.itemIds))
            container.notify("Segnato come indossato oggi")
        }
    }

    fun deleteWear(log: WearLog) {
        viewModelScope.launch { container.dao.deleteWear(log) }
    }
}
