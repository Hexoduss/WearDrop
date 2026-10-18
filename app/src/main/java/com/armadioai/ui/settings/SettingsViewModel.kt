package com.armadioai.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armadioai.ArmadioApplication
import com.armadioai.data.AppSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface KeyCheck {
    data object Idle : KeyCheck
    data object Checking : KeyCheck
    data object Ok : KeyCheck
    data class Failed(val message: String) : KeyCheck
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ArmadioApplication).container
    val settings: StateFlow<AppSettings> = container.settings.state

    var keyCheck by mutableStateOf<KeyCheck>(KeyCheck.Idle)
        private set

    fun setApiKey(value: String) {
        container.settings.update { it.copy(apiKey = value.trim()) }
        keyCheck = KeyCheck.Idle
    }

    fun setModel(id: String) {
        container.settings.update { it.copy(model = id) }
        keyCheck = KeyCheck.Idle
    }

    fun setCity(value: String) = container.settings.update { it.copy(city = value) }

    fun setStyleNotes(value: String) = container.settings.update { it.copy(styleNotes = value) }

    fun verifyKey() {
        val current = settings.value
        if (current.apiKey.isBlank()) {
            keyCheck = KeyCheck.Failed("Inserisci prima la chiave.")
            return
        }
        keyCheck = KeyCheck.Checking
        viewModelScope.launch {
            keyCheck = try {
                container.claude.verifyKey(current.apiKey, current.model)
                KeyCheck.Ok
            } catch (e: Exception) {
                KeyCheck.Failed(e.message ?: "Verifica non riuscita")
            }
        }
    }
}
