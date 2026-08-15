package dev.mopuri.qsactions.shizuku

import android.content.Context
import android.content.Intent

/**
 * Builds the authenticated START/STOP broadcasts understood by thedjchi's Shizuku
 * fork (`ManualStartReceiver` / `ManualStopReceiver`, both extending
 * `AuthenticatedReceiver`).
 *
 * The fork derives its action strings from its own `BuildConfig.APPLICATION_ID`, so
 * with Stealth mode on, [shizukuPackage] is the randomized clone package
 * (e.g. `moe.shizuku.privileged.api.p1k65`) rather than the upstream one.
 */
object ShizukuControl {

    const val START = "START"
    const val STOP = "STOP"

    /** The extra `AuthenticatedReceiver` compares against the token in Shizuku's settings. */
    const val EXTRA_AUTH = "auth"

    const val DEFAULT_PACKAGE = "moe.shizuku.privileged.api"

    /**
     * Explicitly package-scoped: an implicit broadcast would never reach a manifest
     * receiver, and scoping also keeps the auth token off the public bus.
     */
    fun buildIntent(shizukuPackage: String, action: String, authToken: String): Intent =
        Intent("$shizukuPackage.$action")
            .setPackage(shizukuPackage)
            .putExtra(EXTRA_AUTH, authToken)

    fun send(context: Context, shizukuPackage: String, action: String, authToken: String) {
        context.sendBroadcast(buildIntent(shizukuPackage, action, authToken))
    }
}
