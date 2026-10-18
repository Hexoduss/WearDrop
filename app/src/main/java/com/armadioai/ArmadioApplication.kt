package com.armadioai

import android.app.Application
import android.content.Context
import com.armadioai.ai.ClaudeClient
import com.armadioai.data.AppDatabase
import com.armadioai.data.SettingsStore
import com.armadioai.data.WardrobeRepository
import com.armadioai.weather.WeatherClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ArmadioApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Oggetti condivisi da tutta l'app (dependency injection "fatta a mano"). */
class AppContainer(context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    val dao = AppDatabase.build(context).dao()
    val settings = SettingsStore(context)
    val claude = ClaudeClient(settings, http, json)
    val weather = WeatherClient(http, json)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Messaggi brevi mostrati in basso (snackbar). */
    val messages: SharedFlow<String> = _messages

    fun notify(message: String) {
        _messages.tryEmit(message)
    }

    /** Capo da cui far partire lo Stylist (scelto dalla scheda del capo). */
    val stylistAnchor = MutableStateFlow<Long?>(null)

    val repository = WardrobeRepository(context, dao, claude, appScope, ::notify)

    init {
        appScope.launch { dao.markInterruptedAnalyses() }
    }
}
