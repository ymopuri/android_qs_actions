package com.ymopuri.qsactions.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemePrefsTest {

    private val prefs = ThemePrefs.get(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `unknown or missing names resolve to System`() {
        assertEquals(ThemeMode.System, ThemeMode.fromName(null))
        assertEquals(ThemeMode.System, ThemeMode.fromName("Sepia"))
    }

    @Test
    fun `known names round-trip`() {
        assertEquals(ThemeMode.Dark, ThemeMode.fromName(ThemeMode.Dark.name))
    }

    @Test
    fun `setting a mode publishes it on the flow`() {
        prefs.setMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, prefs.mode.value)

        prefs.setMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, prefs.mode.value)
    }
}
