package dev.mopuri.qsactions.shizuku

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShizukuControlTest {

    @Test
    fun `action string is derived from the shizuku package`() {
        val intent = ShizukuControl.buildIntent(STEALTH_PACKAGE, ShizukuControl.START, TOKEN)

        assertEquals("$STEALTH_PACKAGE.START", intent.action)
    }

    @Test
    fun `intent is scoped to the shizuku package`() {
        // Implicit broadcasts never reach manifest receivers, so this is load-bearing.
        val intent = ShizukuControl.buildIntent(STEALTH_PACKAGE, ShizukuControl.STOP, TOKEN)

        assertEquals(STEALTH_PACKAGE, intent.`package`)
    }

    @Test
    fun `auth token travels in the auth extra`() {
        val intent = ShizukuControl.buildIntent(STEALTH_PACKAGE, ShizukuControl.STOP, TOKEN)

        assertEquals(TOKEN, intent.getStringExtra(ShizukuControl.EXTRA_AUTH))
    }

    @Test
    fun `upstream package builds upstream action strings`() {
        val intent = ShizukuControl.buildIntent(
            ShizukuControl.DEFAULT_PACKAGE,
            ShizukuControl.START,
            TOKEN,
        )

        assertEquals("moe.shizuku.privileged.api.START", intent.action)
    }

    private companion object {
        const val STEALTH_PACKAGE = "moe.shizuku.privileged.api.p1k65"
        const val TOKEN = "test-token"
    }
}
