package com.ymopuri.qsactions.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.ymopuri.qsactions.action.ActionRegistry
import com.ymopuri.qsactions.action.QsAction
import java.util.function.Consumer

object TileEnabler {

    /**
     * Whether the system can be asked to add a specific tile. Android 12 has no
     * such API, so there the user adds it from the Quick Settings editor instead —
     * which works as soon as the component is enabled.
     */
    val canRequestAddTile: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

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
     * Re-check every action. Configured-ness can change out of band — a permission
     * granted over adb, say — so this runs on resume rather than only on save.
     */
    fun syncAll(context: Context) {
        ActionRegistry.actions(context).forEach { syncTileEnabled(context, it) }
    }

    /**
     * Ask the system to add this specific tile, rather than telling the user to go
     * edit Quick Settings by hand. The component must be enabled first.
     *
     * The label shown in the system prompt is the user's chosen tile name, so what
     * they typed in the rename dialog is what they're asked to confirm.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestAddTile(context: Context, action: QsAction, onResult: (Int) -> Unit) {
        val statusBar = context.getSystemService(StatusBarManager::class.java)
        if (statusBar == null) {
            onResult(StatusBarManager.TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE)
            return
        }
        statusBar.requestAddTileService(
            action.tileComponent,
            TileLabelStore.get(context).labelFor(action),
            Icon.createWithResource(context, action.tileIconRes),
            context.mainExecutor,
            Consumer<Int> { result -> onResult(result) },
        )
    }
}
