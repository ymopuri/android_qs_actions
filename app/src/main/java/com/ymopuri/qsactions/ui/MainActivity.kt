package com.ymopuri.qsactions.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ymopuri.qsactions.action.ActionRegistry
import com.ymopuri.qsactions.prefs.ThemePrefs
import com.ymopuri.qsactions.tile.TileEnabler

/** Three destinations don't justify a navigation library. */
sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class ActionConfig(val actionId: String) : Screen
}

private val ScreenSaver = Saver<Screen, String>(
    save = { screen ->
        when (screen) {
            Screen.Home -> "home"
            Screen.Settings -> "settings"
            is Screen.ActionConfig -> "action:${screen.actionId}"
        }
    },
    restore = { key ->
        when {
            key == "settings" -> Screen.Settings
            key.startsWith("action:") -> Screen.ActionConfig(key.removePrefix("action:"))
            else -> Screen.Home
        }
    },
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePrefs = remember { ThemePrefs.get(this) }
            val themeMode by themePrefs.mode.collectAsStateWithLifecycle()

            QsActionsTheme(themeMode = themeMode) {
                val actions = remember { ActionRegistry.actions(this) }
                var screen by rememberSaveable(stateSaver = ScreenSaver) {
                    mutableStateOf<Screen>(Screen.Home)
                }

                // Keep tile components in step with whether their action can run.
                LaunchedEffect(Unit) {
                    actions.forEach { TileEnabler.syncTileEnabled(this@MainActivity, it) }
                }

                // Without this the system back gesture leaves the app entirely
                // instead of returning to the list.
                BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

                AppNavHost(
                    screen = screen,
                    actions = actions,
                    onScreenChange = { screen = it },
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(
    screen: Screen,
    actions: List<com.ymopuri.qsactions.action.QsAction>,
    onScreenChange: (Screen) -> Unit,
) {
    when (screen) {
        Screen.Home -> HomeScreen(
            actions = actions,
            onConfigure = { onScreenChange(Screen.ActionConfig(it.id)) },
            onOpenSettings = { onScreenChange(Screen.Settings) },
        )

        Screen.Settings -> SettingsScreen(onNavigateUp = { onScreenChange(Screen.Home) })

        is Screen.ActionConfig -> {
            val action = actions.firstOrNull { it.id == screen.actionId }
            if (action != null && action.hasConfig) {
                action.ConfigContent(onNavigateUp = { onScreenChange(Screen.Home) })
            } else {
                // Restored state pointing at an action that no longer exists.
                LaunchedEffect(screen) { onScreenChange(Screen.Home) }
            }
        }
    }
}
