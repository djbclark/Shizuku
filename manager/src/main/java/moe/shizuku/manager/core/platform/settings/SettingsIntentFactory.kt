package moe.shizuku.manager.core.platform.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

class SettingsIntentFactory(private val context: Context) {
    fun developerOptions(highlight: DeveloperOptionsSetting? = null): Intent =
        buildSettingsIntent(
            Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            highlight?.key
        )

    @RequiresApi(Build.VERSION_CODES.R)
    fun wirelessDebugging(): Intent {
        val intent = buildSettingsIntent(TileService.ACTION_QS_TILE_PREFERENCES).apply {
            val packageName = "com.android.settings"
            setPackage(packageName)
            putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                ComponentName(
                    packageName,
                    $$"com.android.settings.development.qstile.DevelopmentTiles$WirelessDebugging",
                )
            )
        }

        val isIntentResolvable = intent.resolveActivity(context.packageManager) != null

        return if (isIntentResolvable) intent
        else developerOptions(highlight = DeveloperOptionsSetting.WirelessDebugging)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun notifications(channelId: String? = null): Intent =
        buildSettingsIntent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            channelId?.let { putExtra(Settings.EXTRA_CHANNEL_ID, it) }
        }

    fun accessibility(): Intent =
        buildSettingsIntent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    private fun buildSettingsIntent(
        action: String,
        highlight: String? = null
    ): Intent =
        Intent(action).apply {
            highlight?.let { putExtra(":settings:fragment_args_key", it) }
            flags = defaultFlags
        }

    private val defaultFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_NO_HISTORY or
            Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
}

// Enums for mapping a page's setting to its fragment arg key

enum class DeveloperOptionsSetting(val key: String) {
    UsbDebugging("enabled_adb"),
    WirelessDebugging("toggle_adb_wireless")
}
