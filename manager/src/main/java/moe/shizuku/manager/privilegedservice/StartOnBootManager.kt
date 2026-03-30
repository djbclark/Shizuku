package moe.shizuku.manager.privilegedservice

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.core.android.receivers.BootCompleteReceiver
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.utils.EnvironmentUtils

class StartOnBootManager(
    private val context: Context,
    preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _bootReceiverEnabled = MutableStateFlow(false)
    val bootReceiverEnabled: StateFlow<Boolean> = _bootReceiverEnabled.asStateFlow()

    init {
        preferencesRepository.startOnBoot.flow.onEach {
            setBootReceiverEnabled(context, it)
            _bootReceiverEnabled.value = isBootReceiverEnabled(context)
        }.launchIn(scope)
    }

    val canStartOnBoot: Boolean
        get() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                    context.isTelevision ||
                    EnvironmentUtils.isRooted()
        }

    val adbAuthNeverSaved: Boolean
        get() = !context.isTelevision && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    fun isBootReceiverEnabled(context: Context): Boolean {
        val bootCompleteReceiver = ComponentName(context, BootCompleteReceiver::class.java)
        val state = context.packageManager.getComponentEnabledSetting(bootCompleteReceiver)

        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun setBootReceiverEnabled(context: Context, enabled: Boolean) {
        val bootCompleteReceiver = ComponentName(context, BootCompleteReceiver::class.java)
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        context.packageManager.setComponentEnabledSetting(
            bootCompleteReceiver, state, PackageManager.DONT_KILL_APP
        )
    }
}