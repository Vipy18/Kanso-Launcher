package com.kanso.launcher.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.prefs.KansoPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class CityOption(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

class WeatherRepository(
    private val context: Context,
    private val preferences: KansoPreferences
) {
    /**
     * Fetches live weather from Open-Meteo (zero API key required), automatically resolving
     * user's current coarse location + reverse-geocoded city name when location permission is
     * granted and auto-location mode is enabled.
     */
    suspend fun refreshWeather(force: Boolean = false): CachedWeather = withContext(Dispatchers.IO) {
        val current = preferences.settingsFlow.first().weather
        val now = System.currentTimeMillis()
        if (!force && current.updatedAtMs > 0L && now - current.updatedAtMs < 20 * 60 * 1000L) {
            return@withContext current
        }

        val resolved = if (current.isUsingAutoLocation) {
            resolveLiveLocation(current)
        } else {
            Triple(current.latitude, current.longitude, current.cityName)
        }
        val (lat, lon, city) = resolved

        val urlStr =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code"

        val fetched = runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            try {
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val cur = root.getJSONObject("current")
                    CachedWeather(
                        cityName = city,
                        latitude = lat,
                        longitude = lon,
                        temperatureC = cur.optDouble("temperature_2m", current.temperatureC.toDouble()).roundToInt(),
                        humidityPercent = cur.optInt("relative_humidity_2m", current.humidityPercent),
                        weatherCode = cur.optInt("weather_code", current.weatherCode),
                        updatedAtMs = now,
                        isUsingAutoLocation = current.isUsingAutoLocation
                    )
                } else {
                    current
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(current)

        preferences.updateWeather(fetched)
        fetched
    }

    suspend fun searchCities(query: String): List<CityOption> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val urlStr = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json"
        runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            try {
                if (conn.responseCode !in 200..299) return@withContext emptyList()
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
                List(results.length()) { idx ->
                    val obj = results.getJSONObject(idx)
                    CityOption(
                        name = obj.optString("name", trimmed),
                        country = obj.optString("country", ""),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0)
                    )
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun setManualCity(option: CityOption): CachedWeather {
        val current = preferences.settingsFlow.first().weather
        val updated = current.copy(
            cityName = option.name,
            latitude = option.latitude,
            longitude = option.longitude,
            updatedAtMs = 0L,
            isUsingAutoLocation = false
        )
        preferences.updateWeather(updated)
        return refreshWeather(force = true)
    }

    suspend fun enableAutoLocationAndRefresh(): CachedWeather {
        preferences.setUseAutoLocationWeather(true)
        return refreshWeather(force = true)
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolveLiveLocation(fallback: CachedWeather): Triple<Double, Double, String> {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) {
            return Triple(fallback.latitude, fallback.longitude, fallback.cityName)
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Triple(fallback.latitude, fallback.longitude, fallback.cityName)

        val freshLocation: Location? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    lm.allProviders.contains(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider != null) {
                withTimeoutOrNull(4500L) {
                    suspendCancellableCoroutine { cont ->
                        val cancellationSignal = android.os.CancellationSignal()
                        cont.invokeOnCancellation { cancellationSignal.cancel() }
                        runCatching {
                            lm.getCurrentLocation(
                                provider,
                                cancellationSignal,
                                context.mainExecutor
                            ) { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            } else {
                null
            }
        } else {
            null
        }

        val bestLastKnown = freshLocation ?: listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }

        if (bestLastKnown == null) {
            return Triple(fallback.latitude, fallback.longitude, fallback.cityName)
        }

        val resolvedCity = reverseGeocodeCity(bestLastKnown.latitude, bestLastKnown.longitude)
            ?: fallback.cityName
        return Triple(bestLastKnown.latitude, bestLastKnown.longitude, resolvedCity)
    }

    private suspend fun reverseGeocodeCity(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(2500L) {
                suspendCancellableCoroutine { cont ->
                    runCatching {
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            val addr = addresses.firstOrNull()
                            val name = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                            if (cont.isActive) cont.resume(name)
                        }
                    }.onFailure {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
            }.getOrNull()
        }
    }
}
