package com.kanso.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object HomeItemType {
    const val APP = "APP"
    const val FOLDER = "FOLDER"
    const val EXTERNAL_WIDGET = "EXTERNAL_WIDGET"
    const val TELEMETRY_CAPSULE = "TELEMETRY_CAPSULE"
    const val ACTION_MATRIX = "ACTION_MATRIX"
    const val TOGGLE_SWITCH = "TOGGLE_SWITCH"
    const val BATTERY_CAPSULE = "BATTERY_CAPSULE"
    const val MEDIA_HUD = "MEDIA_HUD"
    const val WEATHER_BADGE = "WEATHER_BADGE"
    const val CALENDAR_RIBBON = "CALENDAR_RIBBON"
    const val QUICK_TOGGLE_CLUSTER = "QUICK_TOGGLE_CLUSTER"
    const val ACCENT_BLOCK = "ACCENT_BLOCK"
}

object SurfacePage {
    const val WORKSPACE = 1
    const val LOCKSCREEN = 2

    fun workspacePageIndex(slot: Int): Int = if (slot <= 0) WORKSPACE else 10 + (slot - 1)
}

@Entity(tableName = "home_items")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageIndex: Int = SurfacePage.WORKSPACE,
    val gridCol: Int = 0,
    val gridRow: Int = 0,
    val spanColumns: Int = 1,
    val spanRows: Int = 1,
    val orderIndex: Int = 0,
    val itemType: String = HomeItemType.APP,
    val packageName: String = "",
    val activityName: String = "",
    val label: String = "",
    val folderId: Long? = null,
    val appWidgetId: Int? = null,
    val configJson: String = ""
)

@Entity(tableName = "dock_slots")
data class DockSlotEntity(
    @PrimaryKey val slotIndex: Int,
    val packageName: String,
    val activityName: String,
    val fallbackRole: String
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val folderId: Long = 0,
    val title: String,
    val pageIndex: Int = SurfacePage.WORKSPACE
)

@Entity(tableName = "folder_apps", primaryKeys = ["folderId", "packageName", "activityName"])
data class FolderAppEntity(
    val folderId: Long,
    val packageName: String,
    val activityName: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val packageName: String
)

@Entity(tableName = "icon_overrides")
data class IconOverrideEntity(
    @PrimaryKey val componentKey: String,
    val iconPackPackage: String,
    val drawableName: String
)
