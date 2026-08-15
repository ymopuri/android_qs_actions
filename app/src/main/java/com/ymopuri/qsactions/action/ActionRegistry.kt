package com.ymopuri.qsactions.action

import android.content.Context
import com.ymopuri.qsactions.shizuku.ShizukuToggleAction

/**
 * The list of actions the app offers. This is the only file that changes when a new
 * action is added.
 *
 * Instances are cached per-process so the tile service and the UI observe the same
 * state (both run in the app's main process).
 */
object ActionRegistry {

    @Volatile
    private var cached: List<QsAction>? = null

    fun actions(context: Context): List<QsAction> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: buildActions(context.applicationContext).also { cached = it }
        }
    }

    fun find(context: Context, id: String): QsAction? =
        actions(context).firstOrNull { it.id == id }

    private fun buildActions(appContext: Context): List<QsAction> = listOf(
        ShizukuToggleAction(appContext),
    )
}
