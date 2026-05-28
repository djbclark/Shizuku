package moe.shizuku.manager.autostart

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.autostart.receivers.BootCompleteReceiver
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.utils.root.RootUtils

class StartOnBootManager(
    private val context: Context,
    private val adbSettingsManager: AdbSettingsManager,
    scope: CoroutineScope,
    preferencesRepository: PreferencesRepository
) {

    init {
        preferencesRepository.startOnBoot.flow.onEach {
            setBootReceiverEnabled(it)
        }.launchIn(scope)
    }

    val canStartOnBoot: Boolean
        get() = adbSettingsManager.hasWirelessDebugging ||
                context.isTelevision ||
                (RootUtils.isRooted() ?: false)

    val hasAdbAuthBug: Boolean
        get() = !context.isTelevision && !AndroidVersion.isAtLeast13

    private fun setBootReceiverEnabled(enabled: Boolean) {
        val bootCompleteReceiver = ComponentName(context, BootCompleteReceiver::class.java)
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        context.packageManager.setComponentEnabledSetting(
            bootCompleteReceiver, state, PackageManager.DONT_KILL_APP
        )
    }
}