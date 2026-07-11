package moe.shizuku.manager.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku

class HeadlessStartStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HEADLESS_START -> {
                Log.i(AppConstants.TAG, "Headless start requested")
                val launchMode = ShizukuSettings.getLastLaunchMode()
                if (launchMode == ShizukuSettings.LaunchMethod.ADB) {
                    tryEnsureWirelessAdb(context)
                    AdbStarter.startDirect(context, ShizukuSettings.getTcpPort())
                } else {
                    ShizukuReceiverStarter.start(context, forceStart = true)
                }
            }
            ACTION_HEADLESS_STOP -> {
                Log.i(AppConstants.TAG, "Headless stop requested")
                if (!ShizukuStateMachine.isRunning()) return
                ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                runCatching { Shizuku.exit() }
            }
        }
    }

    private fun tryEnsureWirelessAdb(context: Context) {
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(AppConstants.TAG, "Cannot enable wireless ADB: WRITE_SECURE_SETTINGS not granted")
            return
        }
        try {
            val cr = context.contentResolver
            if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 0) {
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Log.i(AppConstants.TAG, "Enabled ADB")
            }
            if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 0) {
                Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                Log.i(AppConstants.TAG, "Enabled wireless ADB")
            }
        } catch (e: SecurityException) {
            Log.w(AppConstants.TAG, "WRITE_SECURE_SETTINGS permission denied", e)
        } catch (e: Exception) {
            Log.w(AppConstants.TAG, "Failed to ensure wireless ADB", e)
        }
    }

    companion object {
        val ACTION_HEADLESS_START = "${BuildConfig.APPLICATION_ID}.HEADLESS_START"
        val ACTION_HEADLESS_STOP = "${BuildConfig.APPLICATION_ID}.HEADLESS_STOP"
    }
}
