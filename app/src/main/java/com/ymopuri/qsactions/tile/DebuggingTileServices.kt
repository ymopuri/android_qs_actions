package com.ymopuri.qsactions.tile

import com.ymopuri.qsactions.debugging.DebuggingActions

// One subclass per tile: TileServices are manifest components and can't be
// created at runtime. All behaviour lives in BaseActionTileService.

class DeveloperOptionsTileService : BaseActionTileService() {
    override val actionId: String = DebuggingActions.DEVELOPER_OPTIONS_ID
}

class UsbDebuggingTileService : BaseActionTileService() {
    override val actionId: String = DebuggingActions.USB_DEBUGGING_ID
}

class WirelessDebuggingTileService : BaseActionTileService() {
    override val actionId: String = DebuggingActions.WIRELESS_DEBUGGING_ID
}
