package com.ymopuri.qsactions.debugging

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.DrawableRes
import com.ymopuri.qsactions.action.ActionState
import com.ymopuri.qsactions.action.QsAction
import com.ymopuri.qsactions.system.SecureSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A Quick Settings toggle over one `Settings.Global` switch.
 *
 * State is read straight from the setting and observed with a ContentObserver, so
 * the tile tracks changes made anywhere — the Settings app, adb, Shizuku's own
 * start-up — not just changes made here.
 */
class GlobalSettingToggleAction(
    private val context: Context,
    override val id: String,
    override val title: String,
    override val summary: String,
    @DrawableRes override val tileIconRes: Int,
    tileService: Class<*>,
    private val key: String,
    override val onMessage: String,
    override val offMessage: String,
    /** Shown when the system reverted the write; null means no useful hint exists. */
    private val vetoHint: String?,
    private val write: (Context, Boolean) -> Result<Unit>,
) : QsAction {

    override val tileComponent: ComponentName = ComponentName(context, tileService)

    /** Non-null while a toggle is in flight; holds the state we're trying to reach. */
    private val pending = MutableStateFlow<Boolean?>(null)

    override fun isConfigured(): Boolean = SecureSettings.canWrite(context)

    override fun state(): Flow<ActionState> =
        combine(valueFlow(), pending) { on, target ->
            when {
                !isConfigured() -> ActionState.Unavailable("Needs permission — see Settings")
                target != null && on != target -> ActionState.Working
                on -> ActionState.On
                else -> ActionState.Off
            }
        }.distinctUntilChanged()

    override suspend fun toggle(): Result<Unit> {
        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException(
                    "Grant the write secure settings permission in Settings first"
                )
            )
        }

        val target = !read()
        pending.value = target
        try {
            write(context, target).onFailure { return Result.failure(it) }

            // Android can veto a write after accepting it — wireless debugging with no
            // Wi-Fi flips straight back off. Check that it held rather than reporting
            // success on the strength of the write alone.
            delay(SETTLE_MS)
            if (read() != target) {
                return Result.failure(IllegalStateException(vetoMessage(target)))
            }
            return Result.success(Unit)
        } finally {
            pending.value = null
        }
    }

    private fun vetoMessage(target: Boolean): String = when {
        target && vetoHint != null -> "$title didn't stay on. $vetoHint"
        target -> "$title didn't stay on"
        // Turning off and having it come straight back is almost always Shizuku's
        // Watchdog restarting Shizuku, which re-enables debugging to do so.
        else -> "$title came back on. If Shizuku's Watchdog is enabled, stop Shizuku " +
            "from its tile first."
    }

    private fun read(): Boolean = SecureSettings.isEnabled(context, key)

    private fun valueFlow(): Flow<Boolean> = callbackFlow {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(read())
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(key), false, observer)
        trySend(read())
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    private companion object {
        const val SETTLE_MS = 1_500L
    }
}
