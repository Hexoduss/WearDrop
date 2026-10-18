package com.weardrop.weather

import androidx.annotation.StringRes
import com.weardrop.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.roundToInt

data class Weather(
    val place: String,
    val temperature: Int,
    val min: Int,
    val max: Int,
    val rainChance: Int?,
    val code: Int,
) {
    @get:StringRes
    val description: Int get() = describe(code)

    val emoji: String get() = emojiFor(code)

    /** Rain, drizzle, showers or storm now, or a high chance of rain today. */
    val rainy: Boolean get() = code in 51..67 || code in 80..82 || code in 95..99 || (rainChance ?: 0) >= 50
}

class CityNotFoundException : IOException()

/** Free forecast from Open-Meteo: no key, no account. */
class WeatherClient(private val http: OkHttpClient, private val json: Json) {

    @Volatile private var cached: Weather? = null
    @Volatile private var cachedKey: String = ""
    @Volatile private var cachedAt: Long = 0

    suspend fun current(city: String, language: String, force: Boolean = false): Weather = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val key = "${city.lowercase()}|$language"
        val hit = cached
        if (!force && hit != null && cachedKey == key && now - cachedAt < CACHE_MS) {
            return@withContext hit
        }

        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", city)
            .addQueryParameter("count", "1")
            .addQueryParameter("language", language)
            .addQueryParameter("format", "json")
            .build()
        val place = getJson(geoUrl)["results"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw CityNotFoundException()

        val lat = place.getValue("latitude").jsonPrimitive.double
        val lon = place.getValue("longitude").jsonPrimitive.double
        val name = place["name"]?.jsonPrimitive?.content ?: city

        val forecastUrl = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", lat.toString())
            .addQueryParameter("longitude", lon.toString())
            .addQueryParameter("current", "temperature_2m,weather_code")
            .addQueryParameter("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max")
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("forecast_days", "1")
            .build()
        val forecast = getJson(forecastUrl)
        val current = forecast.getValue("current").jsonObject
        val daily = forecast.getValue("daily").jsonObject

        val weather = Weather(
            place = name,
            temperature = current.getValue("temperature_2m").jsonPrimitive.double.roundToInt(),
            min = daily.getValue("temperature_2m_min").jsonArray[0].jsonPrimitive.double.roundToInt(),
            max = daily.getValue("temperature_2m_max").jsonArray[0].jsonPrimitive.double.roundToInt(),
            rainChance = daily["precipitation_probability_max"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.intOrNull,
            code = current.getValue("weather_code").jsonPrimitive.int,
        )
        cached = weather
        cachedKey = key
        cachedAt = now
        weather
    }

    private fun getJson(url: HttpUrl): JsonObject {
        val request = Request.Builder().url(url).build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
        }
    }

    private companion object {
        const val CACHE_MS = 30 * 60 * 1000L
    }
}

/** WMO weather codes used by Open-Meteo. */
@StringRes
private fun describe(code: Int): Int = when (code) {
    0 -> R.string.wx_clear
    1, 2 -> R.string.wx_partly
    3 -> R.string.wx_overcast
    45, 48 -> R.string.wx_fog
    in 51..57 -> R.string.wx_drizzle
    in 61..67 -> R.string.wx_rain
    in 71..77 -> R.string.wx_snow
    in 80..82 -> R.string.wx_showers
    85, 86 -> R.string.wx_snow_showers
    in 95..99 -> R.string.wx_storm
    else -> R.string.wx_variable
}

private fun emojiFor(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..67, in 80..82 -> "🌧️"
    in 71..77, 85, 86 -> "❄️"
    in 95..99 -> "⛈️"
    else -> "🌤️"
}
