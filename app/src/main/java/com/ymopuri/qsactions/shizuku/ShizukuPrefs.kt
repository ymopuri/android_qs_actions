package com.ymopuri.qsactions.shizuku

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Config for the Shizuku action, mirroring the three fields on the fork's Automation
 * card: Action, Package, Extras.
 *
 * [actionPrefix] and [shizukuPackage] are stored separately on purpose — see
 * [ShizukuControl] for why they diverge on a Stealth-mode install.
 *
 * The fork's receivers are exported with no `android:permission`, so [authToken] is
 * the only thing guarding them. It lives in app-private storage and is never logged.
 * (Plain SharedPreferences rather than EncryptedSharedPreferences — `security-crypto`
 * is deprecated and still alpha, and app-private storage already blocks other apps.)
 */
class ShizukuPrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("shizuku", Context.MODE_PRIVATE)

    private val _revision = MutableStateFlow(0)

    /** Bumped on every write so the UI and tile re-read without polling prefs. */
    val revision: StateFlow<Int> = _revision

    /** The Automation card's "Action", minus the trailing `.START` / `.STOP`. */
    val actionPrefix: String
        get() = prefs.getString(KEY_ACTION_PREFIX, ShizukuControl.DEFAULT_PACKAGE)
            .orEmpty()
            .ifBlank { ShizukuControl.DEFAULT_PACKAGE }

    /** The Automation card's "Package". Carries the random suffix under Stealth mode. */
    val shizukuPackage: String
        get() = prefs.getString(KEY_PACKAGE, ShizukuControl.DEFAULT_PACKAGE)
            .orEmpty()
            .ifBlank { ShizukuControl.DEFAULT_PACKAGE }

    val authToken: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()

    /** When on, turning Shizuku off also clears the debugging flags. */
    val lockdown: Boolean
        get() = prefs.getBoolean(KEY_LOCKDOWN, false)

    val isConfigured: Boolean
        get() = actionPrefix.isNotBlank() && shizukuPackage.isNotBlank() && authToken.isNotBlank()

    fun save(actionPrefix: String, shizukuPackage: String, authToken: String) {
        prefs.edit()
            .putString(KEY_ACTION_PREFIX, actionPrefix.trim().removeSuffix("."))
            .putString(KEY_PACKAGE, shizukuPackage.trim())
            .putString(KEY_TOKEN, authToken.trim())
            .apply()
        _revision.value++
    }

    fun setLockdown(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCKDOWN, enabled).apply()
        _revision.value++
    }

    private companion object {
        const val KEY_ACTION_PREFIX = "action_prefix"
        const val KEY_PACKAGE = "package"
        const val KEY_TOKEN = "token"
        const val KEY_LOCKDOWN = "lockdown"
    }
}
