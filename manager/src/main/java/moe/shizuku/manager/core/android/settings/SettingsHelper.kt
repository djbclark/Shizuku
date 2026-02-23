package moe.shizuku.manager.core.android.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri

object SettingsHelper {

    fun launchOrHighlightWirelessDebugging(context: Context) {
        val adbEnabled =
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
        if (adbEnabled > 0) {
            SettingsPage.Developer.WirelessDebugging.launch(context)
        } else SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(
        context: Context,
        launcher: ActivityResultLauncher<Intent>? = null
    ) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = ("package:" + context.packageName).toUri()
        }
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            context.startActivity(intent)
        }
    }

}