package com.kanso.launcher.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class MechaWidgetTheme(
    val title: String,
    val shortCode: String,
    val subtitle: String
) {
    DEFAULT(
        title = "Kanso Neumorphic",
        shortCode = "NEU",
        subtitle = "Primary soft tactile neumorphism with dual cast shadows & rounded depth"
    ),
    MACHINED_MECHA(
        title = "Machined Mecha",
        shortCode = "MCH",
        subtitle = "Chamfered brushed alloy plate with neumorphic shadows & accent brackets"
    ),
    REACTOR_CORE(
        title = "Reactor Core",
        shortCode = "CORE",
        subtitle = "Armored octagon bezel, rotating accent containment ring & neumorphic depth"
    ),
    TACTICAL_GAUGE(
        title = "Tactical Gauge",
        shortCode = "TACT",
        subtitle = "Notched rangefinder chassis, Vernier ticks & neumorphic bevels"
    ),
    DATA_STREAM(
        title = "Data Stream",
        shortCode = "DATA",
        subtitle = "Stepped blade housing, sweeping accent scan beam & neumorphic shadows"
    )
}

@Serializable
enum class AccentBlockStyle(
    val title: String,
    val shortCode: String
) {
    S_CURVE_SHELF("S-Curve Shelf", "CURV"),
    CHAMFERED_PLATE("Chamfered Plate", "CHMF"),
    DIAGONAL_SLAB("Diagonal Armor Slab", "DIAG"),
    CORNER_ARMOR("Corner L-Bracket", "CRNR"),
    STEPPED_MONOLITH("Stepped Monolith", "STEP"),
    SPLIT_CHEVRON("Split Chevron", "CHEV"),
    PILL_BAR("Machined Pill Bar", "PILL")
}

@Serializable
enum class TelemetryMetric(val title: String, val shortCode: String) {
    NET_SPEED("Internet Speed", "NET"),
    RAM_USAGE("RAM Usage", "RAM"),
    STORAGE_USAGE("Internal Storage", "SSD"),
    BATTERY_TEMP("Battery & Temp", "BAT"),
    CPU_LOAD("System Load", "CPU")
}

@Serializable
enum class KansoActionId(val label: String, val shortLabel: String) {
    WIFI("Wi-Fi", "WIFI"),
    MOBILE_DATA("Mobile Data", "DATA"),
    BLUETOOTH("Bluetooth", "BT"),
    HOTSPOT("Hotspot", "SPOT"),
    AIRPLANE("Airplane Mode", "AIR"),
    DND("Do Not Disturb", "DND"),
    TORCH("Flashlight", "TORCH"),
    AUTO_ROTATE("Auto-Rotate", "ROT"),
    POWER_SAVING("Power Saving", "ECO"),
    SILENT_MODE("Sound Mode", "SND"),
    MEDIA_PLAY_PAUSE("Play / Pause", "PLAY"),
    LOCK_SCREEN("Lock Screen", "LOCK"),
    RECENTS("Recents", "TASK"),
    SCREENSHOT("Screenshot", "SNAP"),
    VOLUME_PANEL("Volume HUD", "VOL"),
    BATTERY_SETTINGS("Battery", "BAT"),
    LOCATION("Location GPS", "GPS"),
    NFC("NFC", "NFC"),
    CAST("Screen Cast", "CAST"),
    DARK_MODE("Light / Dark", "MODE"),
    DISPLAY_SETTINGS("Display", "DISP"),
    SYSTEM_SETTINGS("Device Settings", "SYS"),
    OPEN_CLOCK("Clock", "CLK"),
    OPEN_CALENDAR("Calendar", "CAL"),
    OPEN_SETTINGS("Kanso Settings", "CFG"),
    LAUNCH_APP("Launch App", "APP")
}

@Serializable
data class ActionTileBinding(
    val action: KansoActionId = KansoActionId.WIFI,
    val packageName: String = "",
    val activityName: String = "",
    val customLabel: String = ""
)

@Serializable
data class ModularWidgetConfig(
    val mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    val telemetryMetric: TelemetryMetric = TelemetryMetric.NET_SPEED,
    val tapAction: KansoActionId = KansoActionId.WIFI,
    val targetPackage: String = "",
    val targetActivity: String = "",
    val customLabel: String = "",
    val matrixRows: Int = 2,
    val matrixCols: Int = 2,
    val matrixTiles: List<ActionTileBinding> = defaultMatrixTiles(4),
    val toggleAction: KansoActionId = KansoActionId.TORCH,
    val showAudioVisualizer: Boolean = true,
    val accentBlockStyle: AccentBlockStyle = AccentBlockStyle.CHAMFERED_PLATE,
    val accentBlockFlipped: Boolean = false,
    val accentBlockRotated: Boolean = false
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun decode(raw: String): ModularWidgetConfig {
            if (raw.isBlank()) return ModularWidgetConfig()
            return runCatching { json.decodeFromString<ModularWidgetConfig>(raw) }
                .getOrDefault(ModularWidgetConfig())
        }

        fun encode(config: ModularWidgetConfig): String =
            runCatching { json.encodeToString(config) }.getOrDefault("")

        fun decodeTiles(raw: String): List<ActionTileBinding>? {
            if (raw.isBlank()) return null
            return runCatching { json.decodeFromString<List<ActionTileBinding>>(raw) }.getOrNull()
        }

        fun encodeTiles(tiles: List<ActionTileBinding>): String =
            runCatching { json.encodeToString(tiles) }.getOrDefault("")

        fun defaultControlPanelTiles(): List<ActionTileBinding> = listOf(
            ActionTileBinding(KansoActionId.WIFI),
            ActionTileBinding(KansoActionId.MOBILE_DATA),
            ActionTileBinding(KansoActionId.BLUETOOTH),
            ActionTileBinding(KansoActionId.HOTSPOT),
            ActionTileBinding(KansoActionId.AIRPLANE),
            ActionTileBinding(KansoActionId.DND),
            ActionTileBinding(KansoActionId.TORCH),
            ActionTileBinding(KansoActionId.AUTO_ROTATE),
            ActionTileBinding(KansoActionId.POWER_SAVING),
            ActionTileBinding(KansoActionId.SILENT_MODE),
            ActionTileBinding(KansoActionId.RECENTS),
            ActionTileBinding(KansoActionId.OPEN_SETTINGS)
        )

        fun defaultMatrixTiles(count: Int): List<ActionTileBinding> {
            val catalog = listOf(
                KansoActionId.WIFI,
                KansoActionId.MOBILE_DATA,
                KansoActionId.BLUETOOTH,
                KansoActionId.HOTSPOT,
                KansoActionId.AIRPLANE,
                KansoActionId.DND,
                KansoActionId.TORCH,
                KansoActionId.AUTO_ROTATE,
                KansoActionId.POWER_SAVING,
                KansoActionId.SILENT_MODE,
                KansoActionId.MEDIA_PLAY_PAUSE,
                KansoActionId.RECENTS,
                KansoActionId.VOLUME_PANEL,
                KansoActionId.BATTERY_SETTINGS,
                KansoActionId.OPEN_CLOCK,
                KansoActionId.OPEN_SETTINGS
            )
            return List(count.coerceIn(1, 16)) { idx ->
                ActionTileBinding(action = catalog[idx % catalog.size])
            }
        }
    }
}
