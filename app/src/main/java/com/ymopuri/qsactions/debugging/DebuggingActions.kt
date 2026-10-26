package com.ymopuri.qsactions.debugging

import android.content.Context
import com.ymopuri.qsactions.R
import com.ymopuri.qsactions.action.QsAction
import com.ymopuri.qsactions.system.SecureSettings
import com.ymopuri.qsactions.tile.DeveloperOptionsTileService
import com.ymopuri.qsactions.tile.UsbDebuggingTileService
import com.ymopuri.qsactions.tile.WirelessDebuggingTileService

/**
 * The three debugging switches, each its own tile. They share one implementation
 * and differ only in which setting they own and how it cascades — see
 * [SecureSettings] for the cascade rules.
 */
object DebuggingActions {

    const val DEVELOPER_OPTIONS_ID = "developer_options"
    const val USB_DEBUGGING_ID = "usb_debugging"
    const val WIRELESS_DEBUGGING_ID = "wireless_debugging"

    fun all(context: Context): List<QsAction> = listOf(
        developerOptions(context),
        usbDebugging(context),
        wirelessDebugging(context),
    )

    private fun developerOptions(context: Context) = GlobalSettingToggleAction(
        context = context,
        id = DEVELOPER_OPTIONS_ID,
        title = "Developer options",
        summary = "Turning this off also turns off USB and wireless debugging, the " +
            "same as the Settings app does. Needs the write secure settings permission.",
        tileIconRes = R.drawable.ic_tile_developer_options,
        tileService = DeveloperOptionsTileService::class.java,
        key = SecureSettings.DEVELOPMENT_SETTINGS_ENABLED,
        onMessage = "Developer options on",
        offMessage = "Developer options off",
        vetoHint = null,
        write = SecureSettings::setDeveloperOptions,
    )

    private fun usbDebugging(context: Context) = GlobalSettingToggleAction(
        context = context,
        id = USB_DEBUGGING_ID,
        title = "USB debugging",
        summary = "Turning this on also turns on Developer options. Turning it off " +
            "stops an adb-started Shizuku. Needs the write secure settings permission.",
        tileIconRes = R.drawable.ic_tile_usb_debugging,
        tileService = UsbDebuggingTileService::class.java,
        key = SecureSettings.ADB_ENABLED,
        onMessage = "USB debugging on",
        offMessage = "USB debugging off",
        vetoHint = null,
        write = SecureSettings::setUsbDebugging,
    )

    private fun wirelessDebugging(context: Context) = GlobalSettingToggleAction(
        context = context,
        id = WIRELESS_DEBUGGING_ID,
        title = "Wireless debugging",
        summary = "Turning this on also turns on Developer options, and needs a Wi-Fi " +
            "connection. Needs the write secure settings permission.",
        tileIconRes = R.drawable.ic_tile_wireless_debugging,
        tileService = WirelessDebuggingTileService::class.java,
        key = SecureSettings.ADB_WIFI_ENABLED,
        onMessage = "Wireless debugging on",
        offMessage = "Wireless debugging off",
        vetoHint = "It needs a Wi-Fi connection, and the network may need to be " +
            "allowed once from Developer options → Wireless debugging.",
        write = SecureSettings::setWirelessDebugging,
    )
}
