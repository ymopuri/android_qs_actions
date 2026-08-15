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
 * app-wide and future actions are likely to want it. Shizuku is only the
 * *mechanism* by which it gets granted — see
 * [com.ymopuri.qsactions.shizuku.ShizukuShell].
 *
 * Asymmetry worth knowing for the Shizuku action specifically: the fork's
 * `AdbStartWorker` re-enables `adb_enabled` and `adb_wifi_enabled` on start by
 * itself, but nothing in the fork touches `development_settings_enabled`. So this
 * app owns restoring that one before a start, or the start is likely to fail.
 */
object SecureSettings {

    private const val DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled"
    private const val ADB_WIFI_ENABLED = "adb_wifi_enabled"

    fun canWrite(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** The one-time grant. Works because WRITE_SECURE_SETTINGS is a "development" permission. */
    fun grantCommand(packageName: String): String =
        "pm grant $packageName ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    /** Clears the debugging flags. Call *after* whatever needed them has stopped. */
    fun disableDebugging(context: Context): Result<Unit> = write(context) { resolver ->
        Settings.Global.putInt(resolver, ADB_WIFI_ENABLED, 0)
        Settings.Global.putInt(resolver, Settings.Global.ADB_ENABLED, 0)
        Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 0)
    }

    /**
     * Re-opens Developer options. The ADB flags are left alone: Shizuku sets those
     * itself and knows the right order to do it in.
     */
    fun enableDeveloperOptions(context: Context): Result<Unit> = write(context) { resolver ->
        Settings.Global.putInt(resolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
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
