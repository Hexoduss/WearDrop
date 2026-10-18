package com.armadioai.ui.wardrobe

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armadioai.ArmadioApplication
import com.armadioai.data.Category
import com.armadioai.data.ClothingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class WardrobeState(
    val all: List<ClothingItem> = emptyList(),
    val filter: String? = null,
    val loading: Boolean = false,
) {
    val visible: List<ClothingItem>
        get() = if (filter == null) all else all.filter { it.category == filter }

    val categoryCounts: List<Pair<Category, Int>>
        get() = Category.entries.mapNotNull { category ->
            val count = all.count { it.category == category.key }
            if (count > 0) category to count else null
        }
}

class WardrobeViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ArmadioApplication).container
    private val filter = MutableStateFlow<String?>(null)

    val state: StateFlow<WardrobeState> =
        combine(container.repository.items, filter) { items, selected ->
            WardrobeState(all = items, filter = selected)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WardrobeState(loading = true))

    val hasApiKey: StateFlow<Boolean> = container.settings.state
        .map { it.apiKey.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.settings.state.value.apiKey.isNotBlank())

    fun setFilter(key: String?) {
        filter.value = key
    }

    fun addPhotos(uris: List<Uri>) = container.repository.addPhotos(uris)
}
