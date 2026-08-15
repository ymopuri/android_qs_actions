package com.ymopuri.qsactions.shizuku

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import com.ymopuri.qsactions.R
import com.ymopuri.qsactions.system.SecureSettings
import com.ymopuri.qsactions.action.ActionState
import com.ymopuri.qsactions.action.QsAction
import com.ymopuri.qsactions.tile.ShizukuTileService
import com.ymopuri.qsactions.tile.TileEnabler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Starts and stops Shizuku by broadcasting to thedjchi's fork.
 *
 * Stopping goes through the fork's intent rather than killing `shizuku_server`
 * directly — with the fork's Watchdog enabled, a killed pid reads as a crash and gets
 * restarted, so the intent is the only correct way to stop it.
 */
class ShizukuToggleAction(private val context: Context) : QsAction {

    private val prefs = ShizukuPrefs(context)

    /** Non-null while a toggle is in flight; holds the state we're trying to reach. */
    private val pending = MutableStateFlow<Boolean?>(null)

    override val id: String = ID

    override val title: String = "Shizuku control"

    override val summary: String =
        "Start and stop Shizuku from Quick Settings, so banking apps stop complaining. " +
            "Requires thedjchi's Shizuku fork — upstream Shizuku exposes no start/stop " +
            "intent, so this won't work with it."

    override val tileComponent: ComponentName =
        ComponentName(context, ShizukuTileService::class.java)

    override val tileIconRes: Int = R.drawable.ic_tile_shizuku

    override val onMessage: String = "Shizuku started"

    override val offMessage: String = "Shizuku stopped"

    override val hasConfig: Boolean = true

    override fun isConfigured(): Boolean = prefs.isConfigured

    override fun state(): Flow<ActionState> =
        combine(ShizukuState.runningFlow(), pending, prefs.revision) { running, target, _ ->
            when {
                !prefs.isConfigured -> ActionState.Unavailable("Not set up")
                target != null && running != target -> ActionState.Working
                running -> ActionState.On
                else -> ActionState.Off
            }
        }.distinctUntilChanged()

    override suspend fun toggle(): Result<Unit> {
        if (!prefs.isConfigured) {
            return Result.failure(IllegalStateException("Set the Shizuku package and auth token first"))
        }

        val target = !ShizukuState.isRunning()
        pending.value = target
        try {
            if (target) {
                // Shizuku re-enables adb_enabled/adb_wifi_enabled itself, but never
                // development_settings_enabled — so restore that before asking it to start.
                if (prefs.lockdown) SecureSettings.enableDeveloperOptions(context)
                send(ShizukuControl.START)
            } else {
                send(ShizukuControl.STOP)
            }

            val settled = withTimeoutOrNull(if (target) START_TIMEOUT_MS else STOP_TIMEOUT_MS) {
                ShizukuState.runningFlow().first { it == target }
            }
            if (settled == null) {
                return Result.failure(
                    IllegalStateException(
                        if (target) {
                            "Shizuku didn't start. Check Action/Package/token against " +
                                "Shizuku's Automation card, and that Wi-Fi is on if the " +
                                "fork's TCP mode is off."
                        } else {
                            "Shizuku didn't stop. Check Action/Package/token against " +
                                "Shizuku's Automation card — no notification from Shizuku " +
                                "means the broadcast reached no receiver."
                        }
                    )
                )
            }

            // Only clear the debugging flags once Shizuku is actually down.
            if (!target && prefs.lockdown) SecureSettings.disableDebugging(context)
            return Result.success(Unit)
        } finally {
            pending.value = null
        }
    }

    @Composable
    override fun ConfigContent(onNavigateUp: () -> Unit) {
        ShizukuConfigScreen(action = this, onNavigateUp = onNavigateUp)
    }

    internal fun prefs(): ShizukuPrefs = prefs

    internal fun saveConfig(actionPrefix: String, shizukuPackage: String, authToken: String) {
        prefs.save(actionPrefix, shizukuPackage, authToken)
        TileEnabler.syncTileEnabled(context, this)
    }

    private fun send(action: String) {
        ShizukuControl.send(
            context = context,
            actionPrefix = prefs.actionPrefix,
            targetPackage = prefs.shizukuPackage,
            action = action,
            authToken = prefs.authToken,
        )
    }

    companion object {
        const val ID = "shizuku_toggle"

        /** Generous: the fork waits for a Wi-Fi connection when TCP mode is off. */
        private const val START_TIMEOUT_MS = 90_000L
        private const val STOP_TIMEOUT_MS = 20_000L
    }
}
