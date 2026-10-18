package com.armadioai.weather

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
    val description: String get() = describe(code)
    val emoji: String get() = emojiFor(code)

    fun forPrompt(): String = buildString {
        append("$place: ${description.lowercase()}, $temperature°C adesso (min $min°C, max $max°C)")
        rainChance?.let { append(", probabilità di pioggia $it%") }
    }
}

/** Meteo gratuito da Open-Meteo: nessuna chiave richiesta. */
class WeatherClient(private val http: OkHttpClient, private val json: Json) {

    @Volatile private var cached: Weather? = null
    @Volatile private var cachedCity: String = ""
    @Volatile private var cachedAt: Long = 0

    suspend fun current(city: String, force: Boolean = false): Weather = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val hit = cached
        if (!force && hit != null && cachedCity.equals(city, ignoreCase = true) && now - cachedAt < CACHE_MS) {
            return@withContext hit
        }

        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", city)
            .addQueryParameter("count", "1")
            .addQueryParameter("language", "it")
            .addQueryParameter("format", "json")
            .build()
        val place = getJson(geoUrl)["results"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw IOException("Città \"$city\" non trovata")

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
        cachedCity = city
        cachedAt = now
        weather
    }

    private fun getJson(url: HttpUrl): JsonObject {
        val request = Request.Builder().url(url).build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Meteo non disponibile (${response.code})")
            json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
        }
    }

    private companion object {
        const val CACHE_MS = 30 * 60 * 1000L
    }
}

/** Codici meteo WMO usati da Open-Meteo. */
private fun describe(code: Int): String = when (code) {
    0 -> "Sereno"
    1, 2 -> "Poco nuvoloso"
    3 -> "Coperto"
    45, 48 -> "Nebbia"
    in 51..57 -> "Pioviggine"
    in 61..67 -> "Pioggia"
    in 71..77 -> "Neve"
    in 80..82 -> "Rovesci"
    85, 86 -> "Rovesci di neve"
    in 95..99 -> "Temporale"
    else -> "Variabile"
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
