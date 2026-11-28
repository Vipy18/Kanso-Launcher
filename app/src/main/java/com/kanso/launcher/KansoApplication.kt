package com.kanso.launcher

import android.app.Application
import com.kanso.launcher.data.apps.AppRepository
import com.kanso.launcher.data.audio.AudioVisualizerEngine
import com.kanso.launcher.data.db.KansoDatabase
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.prefs.KansoPreferences
import com.kanso.launcher.data.telemetry.TelemetryRepository
import com.kanso.launcher.data.weather.WeatherRepository
import com.kanso.launcher.service.KansoPerformanceGovernor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KansoApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var database: KansoDatabase
        private set
    lateinit var preferences: KansoPreferences
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var telemetryRepository: TelemetryRepository
        private set
    lateinit var weatherRepository: WeatherRepository
        private set
    lateinit var iconPackManager: IconPackManager
        private set
    lateinit var audioVisualizerEngine: AudioVisualizerEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = KansoDatabase.getInstance(this)
        preferences = KansoPreferences(this)
        appRepository = AppRepository(this, appScope)
        telemetryRepository = TelemetryRepository(this, appScope)
        weatherRepository = WeatherRepository(this, preferences)
        iconPackManager = IconPackManager(this)
        audioVisualizerEngine = AudioVisualizerEngine(this, appScope)

        KansoPerformanceGovernor.start(this)
    }

    override fun onTerminate() {
        KansoPerformanceGovernor.stop()
        appRepository.unregister()
        telemetryRepository.unregister()
        audioVisualizerEngine.shutdown()
        super.onTerminate()
    }
}
