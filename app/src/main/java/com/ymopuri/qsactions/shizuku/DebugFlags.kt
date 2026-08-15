package com.ymopuri.qsactions.shizuku

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * The optional lockdown: stopping Shizuku leaves Developer options and USB debugging
 * on, and that — rather than Shizuku itself — is usually what banking apps check.
 *
 * Asymmetry worth knowing: the fork's `AdbStartWorker` re-enables `adb_enabled` and
 * `adb_wifi_enabled` on START by itself, but nothing in the fork touches
 * `development_settings_enabled`. So we own restoring that one before a START, or the
 * start is likely to fail.
 */
object DebugFlags {

    private const val DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled"
    private const val ADB_WIFI_ENABLED = "adb_wifi_enabled"

    fun canWrite(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** The one-time grant. Works because WRITE_SECURE_SETTINGS is a "development" permission. */
    fun grantCommand(packageName: String): String =
        "pm grant $packageName ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    /** Clears debugging flags. Call *after* Shizuku has actually stopped. */
    fun lockDown(context: Context): Result<Unit> = write(context) { resolver ->
        Settings.Global.putInt(resolver, ADB_WIFI_ENABLED, 0)
        Settings.Global.putInt(resolver, Settings.Global.ADB_ENABLED, 0)
        Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 0)
    }

    /**
     * Re-opens Developer options ahead of a START. `adb_enabled` / `adb_wifi_enabled`
     * are left to Shizuku, which sets them itself and knows the right order.
     */
    fun restoreDeveloperOptions(context: Context): Result<Unit> = write(context) { resolver ->
        Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
    }

    private inline fun write(
        context: Context,
        block: (android.content.ContentResolver) -> Unit,
    ): Result<Unit> {
        if (!canWrite(context)) {
            return Result.failure(SecurityException("WRITE_SECURE_SETTINGS not granted"))
        }
        return runCatching { block(context.contentResolver) }
    }
}
