package com.kanso.launcher

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.kanso.launcher.data.apps.AppDrawerCategory
import com.kanso.launcher.data.apps.AppRepository
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.data.discover.DiscoverFeedRepository
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ParsersAndAppRepoTest {

    @Test
    fun discoverRssParser_extractsTitlesSourcesImagesAndSkipsInvalidItems() {
        val sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <item>
                  <title>Next-Gen Orbital Telescope Captures Deep Nebula - Space Daily</title>
                  <link>https://example.com/news/orbit</link>
                  <pubDate>Fri, 27 Nov 2026 14:30:00 GMT</pubDate>
                  <media:thumbnail url="http://ichef.bbci.co.uk/news/240/orbit.jpg" />
                </item>
                <item>
                  <title>Quantum Battery Hits New Density Record</title>
                  <link>https://example.com/news/battery</link>
                  <pubDate>Fri, 27 Nov 2026 10:15:00 GMT</pubDate>
                  <description>&lt;img src="http://cdn.example.com/pic.png?a=1&amp;amp;b=2" /&gt; Summary</description>
                </item>
                <item>
                  <title>Missing Link Should Be Skipped</title>
                  <link></link>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val articles = DiscoverFeedRepository.parseRssXml(sampleRss, "BBC News")
        assertEquals(2, articles.size)

        val first = articles[0]
        assertEquals("Next-Gen Orbital Telescope Captures Deep Nebula", first.title)
        assertEquals("Space Daily", first.source)
        assertEquals("https://ichef.bbci.co.uk/news/240/orbit.jpg", first.imageUrl)
        assertEquals("27 NOV • 14:30", first.pubDate)

        val second = articles[1]
        assertEquals("Quantum Battery Hits New Density Record", second.title)
        assertEquals("BBC News", second.source)
        assertEquals("https://cdn.example.com/pic.png?a=1&b=2", second.imageUrl)
    }

    @Test
    fun weatherJsonParsers_parseForecastAndCityGeocodingAccurately() {
        val forecastJson = """
            {
              "current": {
                "temperature_2m": 18.6,
                "relative_humidity_2m": 71,
                "weather_code": 2
              }
            }
        """.trimIndent()
        val parsed = WeatherRepository.parseCurrentWeatherJson(
            body = forecastJson,
            city = "Kyoto",
            lat = 35.0116,
            lon = 135.7681,
            nowMs = 999_000L,
            fallback = CachedWeather()
        )
        assertEquals("Kyoto", parsed.cityName)
        assertEquals(19, parsed.temperatureC)
        assertEquals(71, parsed.humidityPercent)
        assertEquals(2, parsed.weatherCode)
        assertEquals(999_000L, parsed.updatedAtMs)

        val cityJson = """
            {
              "results": [
                {"name": "Osaka", "country": "Japan", "latitude": 34.6937, "longitude": 135.5023},
                {"name": "Oslo", "country": "Norway", "latitude": 59.9139, "longitude": 10.7522}
              ]
            }
        """.trimIndent()
        val cities = WeatherRepository.parseCitySearchJson(cityJson, "Os")
        assertEquals(2, cities.size)
        assertEquals("Osaka", cities[0].name)
        assertEquals("Japan", cities[0].country)
        assertEquals("Oslo", cities[1].name)

        assertTrue(WeatherRepository.parseCitySearchJson("{}", "Nowhere").isEmpty())
    }

    @Test
    fun appRepository_classifyAppAndSectionLetterCoverAllCategories() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val repo = AppRepository(context, scope)
        try {
            val info = ApplicationInfo()

            assertEquals(AppDrawerCategory.DEV to "DEV", repo.classifyApp(info, "com.termux", "Termux"))
            assertEquals(AppDrawerCategory.MEDIA to "CAM", repo.classifyApp(info, "com.android.camera", "Camera"))
            assertEquals(AppDrawerCategory.SYSTEM to "TIME", repo.classifyApp(info, "com.google.android.deskclock", "Clock"))
            assertEquals(AppDrawerCategory.WORK to "COMMS", repo.classifyApp(info, "com.whatsapp", "WhatsApp"))
            assertEquals(AppDrawerCategory.WORK to "NET", repo.classifyApp(info, "com.android.chrome", "Chrome"))
            assertEquals(AppDrawerCategory.MEDIA to "MEDIA", repo.classifyApp(info, "com.spotify.music", "Spotify"))
            assertEquals(AppDrawerCategory.WORK to "WORK", repo.classifyApp(info, "com.google.android.keep", "Keep Notes"))

            val gameInfo = ApplicationInfo().apply { category = ApplicationInfo.CATEGORY_GAME }
            assertEquals(AppDrawerCategory.GAMES to "GAME", repo.classifyApp(gameInfo, "com.supercell.clash", "Clash"))

            val user = Process.myUserHandle()
            assertEquals('S', InstalledApp("Spotify", "com.spotify.music", "Main", user).sectionLetter)
            assertEquals('#', InstalledApp("1Password", "com.1password", "Main", user).sectionLetter)
            assertEquals('#', InstalledApp("  ", "com.empty", "Main", user).sectionLetter)
        } finally {
            repo.unregister()
            scope.cancel()
        }
    }
}
