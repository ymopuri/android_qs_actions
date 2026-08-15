package dev.mopuri.qsactions.shizuku

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * Whether the Shizuku server is alive.
 *
 * `pingBinder()` only needs the binder to have reached our process — it is not a
 * permission check — so this reports state even though the app never calls a
 * privileged Shizuku API.
 */
object ShizukuState {

    private const val POLL_INTERVAL_MS = 1_500L

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    /**
     * Binder listeners cover the common transitions; the poll is a backstop, since the
     * fork emits no state broadcasts of its own and a binder that was never delivered
     * produces no callback either.
     */
    fun runningFlow(): Flow<Boolean> = callbackFlow {
        val onReceived = Shizuku.OnBinderReceivedListener { trySend(true) }
        val onDead = Shizuku.OnBinderDeadListener { trySend(false) }

        Shizuku.addBinderReceivedListenerSticky(onReceived)
        Shizuku.addBinderDeadListener(onDead)
        trySend(isRunning())

        val poller = launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                trySend(isRunning())
            }
        }

        awaitClose {
            poller.cancel()
            runCatching { Shizuku.removeBinderReceivedListener(onReceived) }
            runCatching { Shizuku.removeBinderDeadListener(onDead) }
        }
    }.distinctUntilChanged()
}
