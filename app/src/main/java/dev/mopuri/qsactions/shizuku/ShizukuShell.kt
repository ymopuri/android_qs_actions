package dev.mopuri.qsactions.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import dev.mopuri.qsactions.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread
import kotlin.coroutines.resume

/**
 * One-shot shell access through Shizuku, used only to self-grant
 * WRITE_SECURE_SETTINGS during setup. Everything else in the app talks to Shizuku
 * over broadcasts and needs no permission at all.
 */
object ShizukuShell {

    private const val PERMISSION_REQUEST_CODE = 4242
    private const val BIND_TIMEOUT_MS = 20_000L

    suspend fun grantWriteSecureSettings(context: Context): Result<String> {
        val command = DebugFlags.grantCommand(context.packageName)
        return exec(context, command)
    }

    suspend fun exec(context: Context, command: String): Result<String> {
        if (!ShizukuState.isRunning()) {
            return Result.failure(IllegalStateException("Shizuku isn't running"))
        }
        if (!ensurePermission()) {
            return Result.failure(SecurityException("Shizuku permission was not granted"))
        }
        return bindAndRun(context, command)
    }

    private suspend fun ensurePermission(): Boolean {
        if (runCatching { Shizuku.isPreV11() }.getOrDefault(true)) return false
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true

        return suspendCancellableCoroutine { continuation ->
            val listener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    if (requestCode != PERMISSION_REQUEST_CODE) return
                    Shizuku.removeRequestPermissionResultListener(this)
                    if (continuation.isActive) {
                        continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }
            Shizuku.addRequestPermissionResultListener(listener)
            continuation.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(listener)
            }
            runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
                .onFailure {
                    Shizuku.removeRequestPermissionResultListener(listener)
                    if (continuation.isActive) continuation.resume(false)
                }
        }
    }

    private suspend fun bindAndRun(context: Context, command: String): Result<String> {
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShellService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

        val result = withTimeoutOrNull(BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        // onServiceConnected lands on the main thread and exec() runs a
                        // shell command, so hop off before calling through.
                        thread {
                            val outcome = runCatching {
                                IShellService.Stub.asInterface(binder).exec(command)
                            }
                            runCatching { Shizuku.unbindUserService(args, this, true) }
                            if (continuation.isActive) continuation.resume(outcome)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) = Unit
                }

                continuation.invokeOnCancellation {
                    runCatching { Shizuku.unbindUserService(args, connection, true) }
                }

                runCatching { Shizuku.bindUserService(args, connection) }
                    .onFailure { if (continuation.isActive) continuation.resume(Result.failure(it)) }
            }
        }

        return result ?: Result.failure(IllegalStateException("Timed out binding the Shizuku shell"))
    }
}
