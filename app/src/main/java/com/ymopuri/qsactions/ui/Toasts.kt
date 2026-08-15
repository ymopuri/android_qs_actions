package com.ymopuri.qsactions.ui

import android.content.Context
import android.widget.Toast

/**
 * Every user-facing message in the app goes through here.
 *
 * A toast rather than a snackbar so in-app feedback matches what the Quick
 * Settings tile can show — a `TileService` has no Compose surface, so a snackbar
 * was never available on that path. The cost is that the OS styles these and the
 * app can't theme them.
 */
fun showToast(context: Context, message: String, long: Boolean = false) {
    Toast.makeText(
        context.applicationContext,
        message,
        if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
    ).show()
}
