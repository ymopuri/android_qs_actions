package com.ymopuri.qsactions.shizuku

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShizukuControlTest {

    @Test
    fun `stealth install keeps the original action while targeting the renamed package`() {
        // The regression this suite exists for. The fork's receivers gate on
        // BuildConfig.APPLICATION_ID, a DEX constant that Stealth mode's package
        // rename does not rewrite — so the action keeps the original name even
        // though the package gains a random suffix. Deriving one from the other
        // sends a broadcast that matches no receiver at all.
        val intent = ShizukuControl.buildIntent(
            actionPrefix = ShizukuControl.DEFAULT_PACKAGE,
            targetPackage = STEALTH_PACKAGE,
            action = ShizukuControl.STOP,
            authToken = TOKEN,
        )

        assertEquals("moe.shizuku.privileged.api.STOP", intent.action)
        assertEquals(STEALTH_PACKAGE, intent.`package`)
    }

    @Test
    fun `action is built from the action prefix`() {
        val intent = ShizukuControl.buildIntent(
            actionPrefix = ShizukuControl.DEFAULT_PACKAGE,
            targetPackage = ShizukuControl.DEFAULT_PACKAGE,
            action = ShizukuControl.START,
            authToken = TOKEN,
        )

        assertEquals("moe.shizuku.privileged.api.START", intent.action)
    }

    @Test
    fun `intent is scoped to the target package`() {
        // Implicit broadcasts never reach manifest receivers, so this is load-bearing.
        val intent = ShizukuControl.buildIntent(
            actionPrefix = ShizukuControl.DEFAULT_PACKAGE,
            targetPackage = STEALTH_PACKAGE,
            action = ShizukuControl.STOP,
            authToken = TOKEN,
        )

        assertEquals(STEALTH_PACKAGE, intent.`package`)
    }

    @Test
    fun `auth token travels in the auth extra`() {
        val intent = ShizukuControl.buildIntent(
            actionPrefix = ShizukuControl.DEFAULT_PACKAGE,
            targetPackage = STEALTH_PACKAGE,
            action = ShizukuControl.STOP,
            authToken = TOKEN,
        )

        assertEquals(TOKEN, intent.getStringExtra(ShizukuControl.EXTRA_AUTH))
    }

    @Test
    fun `a custom action prefix is honoured`() {
        // Covers a rebuilt fork whose baked-in application ID is not the upstream one.
        val intent = ShizukuControl.buildIntent(
            actionPrefix = "com.example.shizuku",
            targetPackage = STEALTH_PACKAGE,
            action = ShizukuControl.START,
            authToken = TOKEN,
        )

        assertEquals("com.example.shizuku.START", intent.action)
        assertEquals(STEALTH_PACKAGE, intent.`package`)
    }

    private companion object {
        const val STEALTH_PACKAGE = "moe.shizuku.privileged.api.p1k65"
        const val TOKEN = "test-token"
    }
}
