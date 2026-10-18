package com.weardrop.ui.item

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.weardrop.R
import com.weardrop.WearDropApplication
import com.weardrop.data.ClothingItem
import com.weardrop.data.NamedColor
import com.weardrop.data.colorList
import com.weardrop.data.toIdList
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

    private val container = (app as WearDropApplication).container
    private val itemId: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    val item: StateFlow<ClothingItem?> = container.repository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val wearStats: StateFlow<WearStats> = container.dao.observeWearLog()
        .map { logs ->
            val mine = logs.filter { itemId in it.itemIds.toIdList() }
            WearStats(count = mine.size, lastWorn = mine.maxOfOrNull { it.wornAt })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearStats())

    /** Editable copy: changes are stored only with "Save". */
    var draft by mutableStateOf<ClothingItem?>(null)
        private set

    init {
        viewModelScope.launch {
            item.collect { current ->
                // Refresh the draft on first load and when the on-device analysis finishes.
                if (current != null && (draft == null || draft?.status != current.status)) {
                    draft = current
                }
            }
        }
    }

    fun edit(transform: (ClothingItem) -> ClothingItem) {
        draft = draft?.let(transform)
    }

    /** Adds or removes a color; the first selected color is the main one. Max 3. */
    fun toggleColor(color: NamedColor) = edit { item ->
        val current = item.colorList()
        val next = if (color in current) current - color else (current + color).take(3)
        item.copy(colors = next.joinToString(",") { it.key })
    }

    fun save() {
        val toSave = draft?.copy(reviewed = true) ?: return
        draft = toSave
        viewModelScope.launch {
            container.dao.updateItem(toSave)
            container.notify(R.string.item_saved)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            container.repository.delete(itemId)
            container.notify(R.string.item_removed)
            onDone()
        }
    }

    fun useAsStylistAnchor() {
        container.stylistAnchor.value = itemId
    }
}
