package com.ymopuri.qsactions.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: System
    }
}

/**
 * App-wide theme choice.
 *
 * Process-singleton so the setting screen and the theme wrapper observe the same
 * flow — a per-call instance would leave the UI showing a stale value until the
 * next recomposition triggered by something else.
 */
class ThemePrefs private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(ThemeMode.fromName(prefs.getString(KEY_MODE, null)))

    val mode: StateFlow<ThemeMode> = _mode

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    companion object {
        private const val KEY_MODE = "theme_mode"

        @Volatile
        private var instance: ThemePrefs? = null

        fun get(context: Context): ThemePrefs =
            instance ?: synchronized(this) {
                instance ?: ThemePrefs(context).also { instance = it }
            }
    }
}
