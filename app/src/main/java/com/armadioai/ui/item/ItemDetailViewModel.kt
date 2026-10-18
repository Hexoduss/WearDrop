package com.armadioai.ui.item

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.armadioai.ArmadioApplication
import com.armadioai.data.ClothingItem
import com.armadioai.data.toIdList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WearStats(val count: Int = 0, val lastWorn: Long? = null)

class ItemDetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val container = (app as ArmadioApplication).container
    private val itemId: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    val item: StateFlow<ClothingItem?> = container.repository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val wearStats: StateFlow<WearStats> = container.dao.observeWearLog()
        .map { logs ->
            val mine = logs.filter { itemId in it.itemIds.toIdList() }
            WearStats(count = mine.size, lastWorn = mine.maxOfOrNull { it.wornAt })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearStats())

    /** Copia modificabile del capo: le modifiche si salvano solo con "Salva". */
    var draft by mutableStateOf<ClothingItem?>(null)
        private set

    init {
        viewModelScope.launch {
            item.collect { current ->
                // Aggiorna la bozza al primo caricamento e quando l'analisi AI termina.
                if (current != null && (draft == null || draft?.status != current.status)) {
                    draft = current
                }
            }
        }
    }

    fun edit(transform: (ClothingItem) -> ClothingItem) {
        draft = draft?.let(transform)
    }

    fun save() {
        val toSave = draft ?: return
        viewModelScope.launch {
            container.dao.updateItem(toSave)
            container.notify("Modifiche salvate")
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            container.repository.delete(itemId)
            container.notify("Capo rimosso dall'armadio")
            onDone()
        }
    }

    fun retryAnalysis() {
        container.repository.retryAnalysis(itemId)
    }

    fun useAsStylistAnchor() {
        container.stylistAnchor.value = itemId
    }
}
