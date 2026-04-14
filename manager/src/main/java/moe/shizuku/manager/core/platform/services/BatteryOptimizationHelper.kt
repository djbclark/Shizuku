package moe.shizuku.manager.core.platform.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import moe.shizuku.manager.core.extensions.isTelevision

class BatteryOptimizationHelper(private val context: Context) {
    private val powerManager: PowerManager by systemService(context)

    fun isIgnoringBatteryOptimizations(): Boolean {
        return context.isTelevision || powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }
}