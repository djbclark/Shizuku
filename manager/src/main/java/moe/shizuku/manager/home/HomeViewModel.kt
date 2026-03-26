package moe.shizuku.manager.home

import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.core.utils.ShizukuSystemApis
import moe.shizuku.manager.home.models.HelpItem
import moe.shizuku.manager.home.models.HomeEvent
import moe.shizuku.manager.shizukuservice.models.ServiceStatus
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Resource
import rikka.shizuku.Shizuku

class HomeViewModel(
    private val shizukuSystemApis: ShizukuSystemApis,
    private val shizukuStateMachine: ShizukuStateMachine,
    private val preferencesRepository: PreferencesRepository,
    private val powerManagerHelper: PowerManagerHelper,
    private val stateMachine: ShizukuStateMachine,
    private val environmentUtils: EnvironmentUtils
) : ViewModel() {

    private val _serviceStatus = MutableLiveData<Resource<ServiceStatus>>()
    val serviceStatus = _serviceStatus as LiveData<Resource<ServiceStatus>>

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    private val shizukuPermissionGroup = "moe.shizuku.manager.permission-group.API"
    private val shizukuPermission = "moe.shizuku.manager.permission.API_V23"

    private fun load(): ServiceStatus {
        environmentUtils.isPermissionOwner(shizukuPermissionGroup, shizukuPermission)
            .onSuccess { isOwner ->
                if (!isOwner) {
                    _events.trySend(HomeEvent.ShowUninstallDialog)
                }
            }
            .onFailure { e ->
                if (e is PackageManager.NameNotFoundException) {
                    _events.trySend(HomeEvent.ShowRebootDialog)
                }
            }

        if (Shizuku.isPreV11() || (Shizuku.getVersion() == 11 && Shizuku.getServerPatchVersion() < 3)) {
            // disable authorized apps
        }

        if (!stateMachine.isRunning()) {
            return ServiceStatus()
        }

        val uid = Shizuku.getUid()
        val apiVersion = Shizuku.getVersion()
        val patchVersion = Shizuku.getServerPatchVersion().let { if (it < 0) 0 else it }
        val seContext = if (apiVersion >= 6) {
            try {
                Shizuku.getSELinuxContext()
            } catch (tr: Throwable) {
                Log.e("HomeViewModel", "getSELinuxContext", tr)
                null
            }
        } else null
        val permissionTest =
            Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED

        val isRunning = uid != -1 && shizukuStateMachine.isRunning()
        shizukuSystemApis.checkPermission(shizukuPermission, environmentUtils.packageName, 0)
        return ServiceStatus(uid, apiVersion, patchVersion, seContext, permissionTest, isRunning)
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = load()
                _serviceStatus.postValue(Resource.success(status))
            } catch (_: CancellationException) {

            } catch (e: Throwable) {
                _serviceStatus.postValue(Resource.error(e, ServiceStatus()))
            }
        }
    }

    fun handleSelectionResult(value: Any) {
        when (value) {
            is HelpItem -> onHelpItemSelected(value)
        }
    }

    private fun onHelpItemSelected(item: HelpItem) {
        val url = when (item) {
            HelpItem.USER_GUIDE -> "https://github.com/thedjchi/Shizuku/wiki"
            HelpItem.TROUBLESHOOTING -> "https://github.com/thedjchi/Shizuku/wiki/troubleshooting"
            HelpItem.BUG_REPORT -> "https://github.com/thedjchi/Shizuku/issues/new?template=bug_report.yml"
            HelpItem.FEATURE_REQUEST -> "https://github.com/thedjchi/Shizuku/issues/new?template=feature_request.yml"
            HelpItem.TRANSLATE -> "https://crowdin.com/project/shizuku"
            HelpItem.EMAIL -> "mailto:thedjchidev@gmail.com"
            HelpItem.PRIVACY -> "https://github.com/thedjchi/Shizuku?tab=readme-ov-file#-privacy"
        }
        _events.trySend(HomeEvent.OpenUrl(url))
    }

    fun checkBatteryOptimization() {
        if (environmentUtils.isTelevision()) return
        if (!preferencesRepository.startOnBoot.get() && !preferencesRepository.watchdog.get()) return
        if (!powerManagerHelper.isIgnoringBatteryOptimizations()) {
            _events.trySend(HomeEvent.ShowBatteryOptimizationSnackbar)
        }
    }

}
