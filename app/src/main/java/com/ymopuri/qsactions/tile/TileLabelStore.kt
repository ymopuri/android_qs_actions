package com.ymopuri.qsactions.tile

import android.content.Context
import com.ymopuri.qsactions.action.QsAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * User-chosen Quick Settings tile names, keyed by action id.
 *
 * Generic on purpose — it lives in `tile/` rather than alongside any one action,
 * so a second action gets renaming for free.
 *
 * SystemUI marquees labels that don't fit rather than truncating them, so there's
 * no length limit to enforce here; a blank override just falls back to the
 * action's title so a tile can never end up nameless.
 */
class TileLabelStore private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("tile_labels", Context.MODE_PRIVATE)

    private val _revision = MutableStateFlow(0)

    /** Bumped on every write so the UI re-reads without polling. */
    val revision: StateFlow<Int> = _revision

    fun labelFor(action: QsAction): String =
        prefs.getString(action.id, null)?.takeIf { it.isNotBlank() } ?: action.title

    fun setLabel(action: QsAction, label: String) {
        val trimmed = label.trim()
        prefs.edit().apply {
            if (trimmed.isBlank()) remove(action.id) else putString(action.id, trimmed)
        }.apply()
        _revision.value++
    }

    companion object {
        @Volatile
        private var instance: TileLabelStore? = null

        fun get(context: Context): TileLabelStore =
            instance ?: synchronized(this) {
                instance ?: TileLabelStore(context).also { instance = it }
            }
    }
}
