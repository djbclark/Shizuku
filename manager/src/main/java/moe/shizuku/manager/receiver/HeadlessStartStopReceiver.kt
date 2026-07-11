package moe.shizuku.manager.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku

class HeadlessStartStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HEADLESS_START -> {
                Log.i(AppConstants.TAG, "Headless start requested")
                val launchMode = ShizukuSettings.getLastLaunchMode()
                if (launchMode == ShizukuSettings.LaunchMethod.ADB || launchMode == ShizukuSettings.LaunchMethod.UNKNOWN) {
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
            ACTION_HEADLESS_STATUS -> {
                val state = ShizukuStateMachine.get()
                val stateLabel = state.name
                val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

                val adbTcpPort = EnvironmentUtils.getAdbTcpPort()
                val adbWifi = runCatching {
                    Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0)
                }.getOrDefault(0)
                val adbUsb = runCatching {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
                }.getOrDefault(0)

                val adbParts = mutableListOf<String>()
                if (adbUsb != 0) adbParts.add("USB:1")
                if (adbWifi != 0) adbParts.add("WiFi:${adbTcpPort.let { if (it > 0) it.toString() else "?" }}")
                if (adbParts.isEmpty()) adbParts.add("off")
                val adbSummary = adbParts.joinToString(" ")

                val summary = "$stateLabel (binder=$binderAlive, ADB: $adbSummary, v${BuildConfig.VERSION_NAME})"

                val extras = Bundle().apply {
                    putString("state", stateLabel)
                    putBoolean("binder_alive", binderAlive)
                    putInt("adb_tcp_port", adbTcpPort)
                    putBoolean("adb_wifi_enabled", adbWifi != 0)
                    putBoolean("adb_enabled", adbUsb != 0)
                    putString("version_name", BuildConfig.VERSION_NAME)
                    putInt("version_code", BuildConfig.VERSION_CODE)
                }

                setResult(state.ordinal, summary, extras)
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
        val ACTION_HEADLESS_STATUS = "${BuildConfig.APPLICATION_ID}.HEADLESS_STATUS"
    }
}
