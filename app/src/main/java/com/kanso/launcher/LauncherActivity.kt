package com.kanso.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.service.KansoLifecycleService
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.drawer.AppDrawerScreen
import com.kanso.launcher.ui.home.HomeScreen
import com.kanso.launcher.ui.settings.SettingsScreen
import com.kanso.launcher.ui.theme.KansoTheme
import com.kanso.launcher.ui.widgets.WidgetMatrixScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object DrawerRoute

@Serializable
data object SettingsRoute

@Serializable
data object WidgetMatrixRoute

class LauncherActivity : ComponentActivity() {

    companion object {
        private const val APPWIDGET_HOST_ID = 0x4B414E53 // "KANS"
    }

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application as KansoApplication)
    }

    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingWidgetLabel: String = "Widget"
    private var pendingWidgetInfo: AppWidgetProviderInfo? = null
    private var pendingWidgetPageIndex: Int = SurfacePage.WORKSPACE

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK &&
            pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            viewModel.addExternalWidgetToHome(
                appWidgetId = pendingWidgetId,
                label = pendingWidgetLabel,
                info = pendingWidgetInfo,
                pageIndex = pendingWidgetPageIndex
            )
        } else if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetInfo = null
        pendingWidgetPageIndex = SurfacePage.WORKSPACE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, APPWIDGET_HOST_ID)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val resolvedDark = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            // Sync system status/navigation bar icon contrast cleanly with Kanso's independent Light/Dark mode
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !resolvedDark
                controller.isAppearanceLightNavigationBars = !resolvedDark
            }

            KansoTheme(
                themeMode = settings.themeMode,
                accentColorArgb = settings.accentColorArgb,
                useMaterialYouAccent = settings.useMaterialYouAccent
            ) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    composable<HomeRoute>(
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() }
                    ) {
                        HomeScreen(
                            viewModel = viewModel,
                            appWidgetHost = appWidgetHost,
                            onRequestBindExternalWidget = ::requestBindExternalWidget,
                            onOpenDrawer = {
                                navController.navigate(DrawerRoute) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenSettings = {
                                navController.navigate(SettingsRoute) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<DrawerRoute>(
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn()
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Down,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeOut()
                        }
                    ) {
                        AppDrawerScreen(
                            viewModel = viewModel,
                            onClose = { navController.popBackStack() },
                            onOpenSettings = {
                                navController.navigate(SettingsRoute) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<SettingsRoute>(
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeIn()
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeOut()
                        }
                    ) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenWidgetMatrix = {
                                navController.navigate(WidgetMatrixRoute) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<WidgetMatrixRoute>(
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeIn()
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeOut()
                        }
                    ) {
                        WidgetMatrixScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { appWidgetHost.startListening() }
        KansoLifecycleService.start(this)
        viewModel.refreshPermissions()
    }

    override fun onStop() {
        runCatching { appWidgetHost.stopListening() }
        super.onStop()
    }

    private fun requestBindExternalWidget(info: AppWidgetProviderInfo, targetPageIndex: Int) {
        val widgetId = appWidgetHost.allocateAppWidgetId()
        val label = info.loadLabel(packageManager) ?: info.provider.shortClassName
        val boundImmediately = runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, info.provider)
        }.getOrDefault(false)

        if (boundImmediately) {
            viewModel.addExternalWidgetToHome(
                appWidgetId = widgetId,
                label = label,
                info = info,
                pageIndex = targetPageIndex
            )
        } else {
            pendingWidgetId = widgetId
            pendingWidgetLabel = label
            pendingWidgetInfo = info
            pendingWidgetPageIndex = targetPageIndex
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            val launched = runCatching {
                bindWidgetLauncher.launch(bindIntent)
                true
            }.getOrDefault(false)
            if (!launched) {
                runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                pendingWidgetInfo = null
                pendingWidgetPageIndex = SurfacePage.WORKSPACE
            }
        }
    }
}
