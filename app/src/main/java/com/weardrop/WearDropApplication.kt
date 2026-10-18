package com.weardrop

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import com.weardrop.analysis.ItemAnalyzer
import com.weardrop.data.AppDatabase
import com.weardrop.data.SettingsStore
import com.weardrop.data.WardrobeRepository
import com.weardrop.weather.WeatherClient
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

class WearDropApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Objects shared across the app (hand-written dependency injection). */
class AppContainer(context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val dao = AppDatabase.build(context).dao()
    val settings = SettingsStore(context)
    val weather = WeatherClient(http, Json { ignoreUnknownKeys = true })

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 16)

    /** Short messages (string resource ids) shown as snackbars. */
    val messages: SharedFlow<Int> = _messages

    fun notify(@StringRes message: Int) {
        _messages.tryEmit(message)
    }

    /** Item the Stylist should build outfits around (picked from the item screen). */
    val stylistAnchor = MutableStateFlow<Long?>(null)

    val repository = WardrobeRepository(context, dao, ItemAnalyzer(), appScope, ::notify)

    init {
        appScope.launch { dao.finishInterruptedAnalyses() }
    }
}
