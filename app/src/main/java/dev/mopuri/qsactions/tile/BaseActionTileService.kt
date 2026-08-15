package dev.mopuri.qsactions.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.mopuri.qsactions.action.ActionRegistry
import dev.mopuri.qsactions.action.ActionState
import dev.mopuri.qsactions.action.QsAction
import dev.mopuri.qsactions.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * All tile behaviour lives here; a subclass only names its action.
 *
 * Tiles can't be created at runtime — each is a manifest component — so a new action
 * needs a subclass and a `<service>` entry, and nothing more.
 */
abstract class BaseActionTileService : TileService() {

    protected abstract val actionId: String

    private val action: QsAction? get() = ActionRegistry.find(this, actionId)

    private var listeningScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val action = action ?: return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listeningScope = scope
        scope.launch {
            action.state().collect { render(action, it) }
        }
    }

    override fun onStopListening() {
        listeningScope?.cancel()
        listeningScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val action = action ?: return

        if (!action.isConfigured()) {
            openApp()
            return
        }

        render(action, ActionState.Working)
        // Deliberately not the listening scope: the QS panel collapses on tap, which
        // ends listening well before a toggle finishes.
        TileScope.scope.launch { action.toggle() }
    }

    private fun render(action: QsAction, state: ActionState) {
        val tile = qsTile ?: return
        tile.label = action.title
        tile.icon = Icon.createWithResource(this, action.tileIconRes)
        when (state) {
            ActionState.On -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = "On"
            }

            ActionState.Off -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = "Off"
            }

            ActionState.Working -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.subtitle = "Working…"
            }

            is ActionState.Unavailable -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.subtitle = state.reason
            }
        }
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

/** Process-scoped so an in-flight toggle survives the QS panel closing. */
private object TileScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
