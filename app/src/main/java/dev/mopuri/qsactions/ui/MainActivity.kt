package dev.mopuri.qsactions.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.mopuri.qsactions.action.ActionRegistry
import dev.mopuri.qsactions.tile.TileEnabler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QsActionsTheme {
                val actions = remember { ActionRegistry.actions(this) }
                var openActionId by rememberSaveable { mutableStateOf<String?>(null) }

                // Keep tile components in step with whether their action can run.
                LaunchedEffect(Unit) {
                    actions.forEach { TileEnabler.syncTileEnabled(this@MainActivity, it) }
                }

                val openAction = actions.firstOrNull { it.id == openActionId }
                if (openAction != null && openAction.hasConfig) {
                    openAction.ConfigContent(onNavigateUp = { openActionId = null })
                } else {
                    HomeScreen(
                        actions = actions,
                        onConfigure = { openActionId = it.id },
                    )
                }
            }
        }
    }
}
