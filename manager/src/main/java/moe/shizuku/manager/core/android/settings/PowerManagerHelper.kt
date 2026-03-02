package moe.shizuku.manager.core.android.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

object PowerManagerHelper {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context
    }

    private val powerManager by lazy {
        appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
    }

    @SuppressLint("BatteryLife")
    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${appContext.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}