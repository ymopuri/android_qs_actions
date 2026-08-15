package com.ymopuri.qsactions.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import com.ymopuri.qsactions.action.QsAction
import java.util.function.Consumer

object TileEnabler {

    /**
     * Tiles ship disabled so the Quick Settings editor only lists ones that work.
     * Called whenever an action's configured-ness changes.
     */
    fun setTileEnabled(context: Context, component: ComponentName, enabled: Boolean) {
        val target = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val pm = context.packageManager
        if (pm.getComponentEnabledSetting(component) == target) return
        pm.setComponentEnabledSetting(component, target, PackageManager.DONT_KILL_APP)
    }

    fun syncTileEnabled(context: Context, action: QsAction) {
        setTileEnabled(context, action.tileComponent, action.isConfigured())
    }

    /**
     * Ask the system to add this specific tile, rather than telling the user to go
     * edit Quick Settings by hand. The component must be enabled first.
     */
    fun requestAddTile(context: Context, action: QsAction, onResult: (Int) -> Unit) {
        val statusBar = context.getSystemService(StatusBarManager::class.java)
        if (statusBar == null) {
            onResult(StatusBarManager.TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE)
            return
        }
        statusBar.requestAddTileService(
            action.tileComponent,
            action.title,
            Icon.createWithResource(context, action.tileIconRes),
            context.mainExecutor,
            Consumer<Int> { result -> onResult(result) },
        )
    }
}
