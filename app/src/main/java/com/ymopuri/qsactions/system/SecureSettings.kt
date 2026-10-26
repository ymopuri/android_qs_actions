package com.ymopuri.qsactions.system

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Everything that needs `WRITE_SECURE_SETTINGS`.
 *
 * Deliberately generic and free of any Shizuku reference: the permission is
 * app-wide and several actions use it. Shizuku is only the *mechanism* by which it
 * gets granted — see [com.ymopuri.qsactions.shizuku.ShizukuShell].
 *
 * Asymmetry worth knowing for the Shizuku action specifically: the fork's
 * `AdbStartWorker` re-enables `adb_enabled` and `adb_wifi_enabled` on start by
 * itself, but nothing in the fork touches `development_settings_enabled`. So this
 * app owns restoring that one before a start, or the start is likely to fail.
 */
object SecureSettings {

    const val DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled"
    const val ADB_ENABLED = Settings.Global.ADB_ENABLED

    /** Hidden in the SDK, but stable since wireless debugging arrived in Android 11. */
    const val ADB_WIFI_ENABLED = "adb_wifi_enabled"

    fun canWrite(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** Reading Settings.Global needs no permission; only writing does. */
    fun isEnabled(context: Context, key: String): Boolean =
        Settings.Global.getInt(context.contentResolver, key, 0) == 1

    /** The one-time grant. Works because WRITE_SECURE_SETTINGS is a "development" permission. */
    fun grantCommand(packageName: String): String =
        "pm grant $packageName ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    // ── Shizuku lockdown ────────────────────────────────────────────────────────

    /** Clears the debugging flags. Call *after* whatever needed them has stopped. */
    fun disableDebugging(context: Context): Result<Unit> = write(context) { resolver ->
        Settings.Global.putInt(resolver, ADB_WIFI_ENABLED, 0)
        Settings.Global.putInt(resolver, ADB_ENABLED, 0)
        Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 0)
    }

    /**
     * Re-opens Developer options. The ADB flags are left alone: Shizuku sets those
     * itself and knows the right order to do it in.
     */
    fun enableDeveloperOptions(context: Context): Result<Unit> = write(context) { resolver ->
        Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
    }

    // ── Individual toggles ──────────────────────────────────────────────────────
    //
    // These mirror the cascades the Settings app itself enforces, so the device
    // never ends up in a state Settings couldn't have produced:
    //  - Developer options off takes USB and wireless debugging with it. Leaving
    //    adbd running behind a hidden menu is exactly what banking apps still flag.
    //  - Either debugging switch on brings Developer options on, since Settings
    //    doesn't let debugging exist without it.

    fun setDeveloperOptions(context: Context, enabled: Boolean): Result<Unit> =
        if (enabled) enableDeveloperOptions(context) else disableDebugging(context)

    fun setUsbDebugging(context: Context, enabled: Boolean): Result<Unit> =
        write(context) { resolver ->
            if (enabled) Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
            Settings.Global.putInt(resolver, ADB_ENABLED, if (enabled) 1 else 0)
        }

    fun setWirelessDebugging(context: Context, enabled: Boolean): Result<Unit> =
        write(context) { resolver ->
            if (enabled) Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
            Settings.Global.putInt(resolver, ADB_WIFI_ENABLED, if (enabled) 1 else 0)
        }

    private inline fun write(
        context: Context,
        block: (ContentResolver) -> Unit,
    ): Result<Unit> {
        if (!canWrite(context)) {
            return Result.failure(SecurityException("WRITE_SECURE_SETTINGS not granted"))
        }
        return runCatching { block(context.contentResolver) }
    }
}
