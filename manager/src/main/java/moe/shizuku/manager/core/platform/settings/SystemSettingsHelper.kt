package moe.shizuku.manager.core.platform.settings

import android.content.Context
import android.provider.Settings

object SystemSettingsHelper {
    fun launchOrHighlightWirelessDebugging(context: Context) {
        val adbEnabled =
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
        if (adbEnabled > 0) {
            SystemSettingsPage.Developer.WirelessDebugging.launch(context)
        } else SystemSettingsPage.Developer.HighlightWirelessDebugging.launch(context)
    }
}