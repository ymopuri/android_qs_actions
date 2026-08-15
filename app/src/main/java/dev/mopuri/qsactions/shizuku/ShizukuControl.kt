package dev.mopuri.qsactions.shizuku

import android.content.Context
import android.content.Intent

/**
 * Builds the authenticated START/STOP broadcasts understood by thedjchi's Shizuku
 * fork (`ManualStartReceiver` / `ManualStopReceiver`, both extending
 * `AuthenticatedReceiver`).
 *
 * **The action prefix and the target package are not the same value, and neither can
 * be derived from the other.** On a Stealth-mode install:
 *
 *  - The package is the randomized clone name (`…privileged.api.p1k65`), because
 *    Stealth mode's `changePackageName()` rewrites the manifest package.
 *  - The action keeps the *original* name (`…privileged.api.STOP`), because the
 *    receivers gate on `BuildConfig.APPLICATION_ID` — a constant compiled into the
 *    DEX, which the rename does not touch.
 *
 * The fork's Automation card shows both, and is authoritative: it renders the action
 * from that same constant. Deriving the action from the package produces a broadcast
 * that matches no receiver, which fails silently — not even the fork's
 * auth-failure notification appears.
 */
object ShizukuControl {

    const val START = "START"
    const val STOP = "STOP"

    /** The extra `AuthenticatedReceiver` compares against the token in Shizuku's settings. */
    const val EXTRA_AUTH = "auth"

    /**
     * Upstream's application ID. Doubles as the default action prefix, since that is
     * the value baked into a stock fork build, and as the default package for a
     * non-Stealth install.
     */
    const val DEFAULT_PACKAGE = "moe.shizuku.privileged.api"

    /**
     * @param actionPrefix what the Automation card lists under "Action", minus the
     *   trailing `.START` / `.STOP`.
     * @param targetPackage what the Automation card lists under "Package". Scoping is
     *   load-bearing: an implicit broadcast would never reach a manifest receiver, and
     *   it also keeps the auth token off the public bus.
     */
    fun buildIntent(
        actionPrefix: String,
        targetPackage: String,
        action: String,
        authToken: String,
    ): Intent =
        Intent("$actionPrefix.$action")
            .setPackage(targetPackage)
            .putExtra(EXTRA_AUTH, authToken)

    fun send(
        context: Context,
        actionPrefix: String,
        targetPackage: String,
        action: String,
        authToken: String,
    ) {
        context.sendBroadcast(buildIntent(actionPrefix, targetPackage, action, authToken))
    }
}
