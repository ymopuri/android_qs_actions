package dev.mopuri.qsactions.shizuku

import android.content.Context
import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * Runs inside a process Shizuku forks for us, so it carries shell (uid 2000)
 * privileges. Used exactly once, to grant this app WRITE_SECURE_SETTINGS.
 *
 * Instantiated reflectively by the Shizuku server — see the keep rule in
 * proguard-rules.pro.
 */
@Keep
class ShellService() : IShellService.Stub() {

    /** Context-taking constructor, available from Shizuku API v13. */
    @Keep
    constructor(context: Context) : this()

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): String {
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        return if (exitCode == 0) output else "exit $exitCode: $output"
    }
}
