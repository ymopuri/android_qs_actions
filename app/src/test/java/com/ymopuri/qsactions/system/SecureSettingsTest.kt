package com.ymopuri.qsactions.system

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecureSettingsTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun grantAndReset() {
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        set(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED, 0)
        set(SecureSettings.ADB_ENABLED, 0)
        set(SecureSettings.ADB_WIFI_ENABLED, 0)
    }

    @Test
    fun `developer options off takes both debugging switches with it`() {
        set(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED, 1)
        set(SecureSettings.ADB_ENABLED, 1)
        set(SecureSettings.ADB_WIFI_ENABLED, 1)

        SecureSettings.setDeveloperOptions(app, false).getOrThrow()

        assertEquals(0, get(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(0, get(SecureSettings.ADB_ENABLED))
        assertEquals(0, get(SecureSettings.ADB_WIFI_ENABLED))
    }

    @Test
    fun `developer options on leaves debugging alone`() {
        SecureSettings.setDeveloperOptions(app, true).getOrThrow()

        assertEquals(1, get(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(0, get(SecureSettings.ADB_ENABLED))
        assertEquals(0, get(SecureSettings.ADB_WIFI_ENABLED))
    }

    @Test
    fun `usb debugging on brings developer options on`() {
        SecureSettings.setUsbDebugging(app, true).getOrThrow()

        assertEquals(1, get(SecureSettings.ADB_ENABLED))
        assertEquals(1, get(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED))
    }

    @Test
    fun `usb debugging off leaves developer options and wireless alone`() {
        set(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED, 1)
        set(SecureSettings.ADB_ENABLED, 1)
        set(SecureSettings.ADB_WIFI_ENABLED, 1)

        SecureSettings.setUsbDebugging(app, false).getOrThrow()

        assertEquals(0, get(SecureSettings.ADB_ENABLED))
        assertEquals(1, get(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(1, get(SecureSettings.ADB_WIFI_ENABLED))
    }

    @Test
    fun `wireless debugging on brings developer options on`() {
        SecureSettings.setWirelessDebugging(app, true).getOrThrow()

        assertEquals(1, get(SecureSettings.ADB_WIFI_ENABLED))
        assertEquals(1, get(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED))
    }

    @Test
    fun `wireless debugging off leaves usb debugging alone`() {
        set(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED, 1)
        set(SecureSettings.ADB_ENABLED, 1)
        set(SecureSettings.ADB_WIFI_ENABLED, 1)

        SecureSettings.setWirelessDebugging(app, false).getOrThrow()

        assertEquals(0, get(SecureSettings.ADB_WIFI_ENABLED))
        assertEquals(1, get(SecureSettings.ADB_ENABLED))
    }

    @Test
    fun `existing lockdown behaviour is unchanged`() {
        // The Shizuku lockdown predates the individual toggles; this pins it so the
        // new cascades can't quietly change what it does.
        set(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED, 1)
        set(SecureSettings.ADB_ENABLED, 1)
        set(SecureSettings.ADB_WIFI_ENABLED, 1)

        SecureSettings.disableDebugging(app).getOrThrow()
        assertEquals(0, get(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(0, get(SecureSettings.ADB_ENABLED))
        assertEquals(0, get(SecureSettings.ADB_WIFI_ENABLED))

        SecureSettings.enableDeveloperOptions(app).getOrThrow()
        assertEquals(1, get(SecureSettings.DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(0, get(SecureSettings.ADB_ENABLED))
    }

    @Test
    fun `writes are refused without the permission`() {
        shadowOf(app).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)

        val result = SecureSettings.setUsbDebugging(app, true)

        assertTrue(result.exceptionOrNull() is SecurityException)
        assertEquals(0, get(SecureSettings.ADB_ENABLED))
    }

    private fun set(key: String, value: Int) {
        Settings.Global.putInt(app.contentResolver, key, value)
    }

    private fun get(key: String): Int = Settings.Global.getInt(app.contentResolver, key, 0)
}
