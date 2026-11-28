package com.kanso.launcher

import android.app.Application
import android.media.AudioManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kanso.launcher.data.model.ActionTileBinding
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.AodContent
import com.kanso.launcher.service.KansoNotificationItem
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.panels.KansoShadeOverlay
import com.kanso.launcher.ui.panels.ShadeStage
import com.kanso.launcher.ui.theme.KansoTheme
import com.kanso.launcher.ui.widgets.KansoWidgetTemplate
import com.kanso.launcher.ui.widgets.ModularWidgetAddDialog
import com.kanso.launcher.ui.widgets.TriStateRingerWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xhdpi", application = Application::class)
class KansoComposeUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shadeOverlay_notificationsStageDisplaysAlertsAndSwitchesToControls() {
        var stageChangedTo: ShadeStage? = null
        var clearedAll = false
        var dismissedKey: String? = null

        val sampleNotifications = listOf(
            KansoNotificationItem(
                key = "notif_1",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Alex",
                text = "Ready for launch?",
                postTimeMs = System.currentTimeMillis() - 10_000L,
                isClearable = true,
                contentIntent = null
            )
        )

        composeRule.setContent {
            KansoTheme(themeMode = ThemeMode.DARK) {
                KansoShadeOverlay(
                    stage = ShadeStage.NOTIFICATIONS,
                    notifications = sampleNotifications,
                    notificationAccessGranted = true,
                    telemetry = TelemetryState(batteryPercent = 88),
                    media = MediaHudState(title = "Nightcall", artist = "Kavinsky", isPlaying = true, hasActiveSession = true),
                    controlTiles = ModularWidgetConfig.defaultControlPanelTiles(),
                    onStageChange = { stageChangedTo = it },
                    onOpenNotificationAccessSettings = {},
                    onDismissNotification = { dismissedKey = it },
                    onClearAllNotifications = { clearedAll = true },
                    onExecuteTile = {},
                    onAddControlTile = {},
                    onUpdateControlTile = { _, _ -> },
                    onRemoveControlTile = {},
                    onResetControlTiles = {},
                    onBrightnessChange = {},
                    onVolumeChange = {},
                    onPlayPause = {},
                    onPrevTrack = {},
                    onNextTrack = {},
                    onOpenPlayerApp = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("kanso_shade_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Alex").assertIsDisplayed()

        composeRule.onNodeWithTag("notifications_clear_all").performClick()
        composeRule.waitForIdle()
        assertTrue(clearedAll)

        composeRule.onNodeWithTag("shade_toggle_stage_button").performClick()
        composeRule.waitForIdle()
        assertEquals(ShadeStage.CONTROLS, stageChangedTo)
    }

    @Test
    fun shadeOverlay_controlsStageExecutesTileAndTogglesEditMode() {
        var executedTile: ActionTileBinding? = null
        var resetCalled = false
        val tiles = listOf(
            ActionTileBinding(KansoActionId.WIFI, "WI-FI"),
            ActionTileBinding(KansoActionId.TORCH, "TORCH")
        )

        composeRule.setContent {
            KansoTheme(themeMode = ThemeMode.DARK) {
                KansoShadeOverlay(
                    stage = ShadeStage.CONTROLS,
                    notifications = emptyList(),
                    notificationAccessGranted = true,
                    telemetry = TelemetryState(isWifiConnected = true),
                    media = MediaHudState(),
                    controlTiles = tiles,
                    onStageChange = {},
                    onOpenNotificationAccessSettings = {},
                    onDismissNotification = {},
                    onClearAllNotifications = {},
                    onExecuteTile = { executedTile = it },
                    onAddControlTile = {},
                    onUpdateControlTile = { _, _ -> },
                    onRemoveControlTile = {},
                    onResetControlTiles = { resetCalled = true },
                    onBrightnessChange = {},
                    onVolumeChange = {},
                    onPlayPause = {},
                    onPrevTrack = {},
                    onNextTrack = {},
                    onOpenPlayerApp = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("control_tile_1_torch").performClick()
        composeRule.waitForIdle()
        assertEquals(KansoActionId.TORCH, executedTile?.action)

        composeRule.onNodeWithTag("control_panel_edit_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("control_panel_reset_tiles").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(resetCalled)
    }

    @Test
    fun triStateRingerWidget_dispatchesAllThreeAudioModes() {
        val selected = mutableListOf<Int>()

        composeRule.setContent {
            KansoTheme(themeMode = ThemeMode.DARK) {
                TriStateRingerWidget(
                    ringerMode = AudioManager.RINGER_MODE_NORMAL,
                    mechaTheme = MechaWidgetTheme.DEFAULT,
                    onSelectRingerMode = { selected.add(it) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tri_state_btn_silent").performClick()
        composeRule.onNodeWithTag("tri_state_btn_vibrate").performClick()
        composeRule.onNodeWithTag("tri_state_btn_ring").performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(
                AudioManager.RINGER_MODE_SILENT,
                AudioManager.RINGER_MODE_VIBRATE,
                AudioManager.RINGER_MODE_NORMAL
            ),
            selected
        )
    }

    @Test
    fun modularWidgetAddDialog_switchesTabsAndPlacesWidget() {
        var placedTemplate: KansoWidgetTemplate? = null

        composeRule.setContent {
            KansoTheme(themeMode = ThemeMode.DARK) {
                ModularWidgetAddDialog(
                    telemetry = TelemetryState(batteryPercent = 75),
                    media = MediaHudState(),
                    weather = CachedWeather(cityName = "Tokyo", temperatureC = 22),
                    audioBands = List(13) { 0.3f },
                    providers = emptyList(),
                    allowExternalWidgets = false,
                    onAddKansoWidget = { placedTemplate = it },
                    onSelectExternalProvider = {},
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("widget_add_preview_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("widget_picker_tab_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("widget_picker_tab_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("place_widget_stitch_clock_DEFAULT").performClick()
        composeRule.waitForIdle()

        assertNotNull(placedTemplate)
    }

    @Test
    fun aodContent_rendersDateAndBatteryPercentage() {
        composeRule.setContent {
            KansoTheme(themeMode = ThemeMode.DARK) {
                AodContent(
                    hours = "09",
                    minutes = "41",
                    date = "Friday, Nov 27",
                    batteryPercent = 91,
                    notificationDotCount = 3
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("FRIDAY, NOV 27").assertIsDisplayed()
        composeRule.onNodeWithText("91%").assertIsDisplayed()
    }
}
