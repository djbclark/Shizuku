package moe.shizuku.manager.home

import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.platform.settings.PowerManagerHelper
import moe.shizuku.manager.home.models.HelpItem
import moe.shizuku.manager.home.models.HomeEvent
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.privilegedservice.models.ServiceStatus
import moe.shizuku.manager.privilegedservice.data.ShizukuStateMachine
import rikka.lifecycle.Resource
import rikka.shizuku.Shizuku

class HomeViewModel(
    private val shizukuStateMachine: ShizukuStateMachine,
    private val preferencesRepository: PreferencesRepository,
    private val powerManagerHelper: PowerManagerHelper,
    private val stateMachine: ShizukuStateMachine,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _serviceStatus = MutableLiveData<Resource<ServiceStatus>>()
    val serviceStatus = _serviceStatus as LiveData<Resource<ServiceStatus>>

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    private fun load(): ServiceStatus {
        permissionManager.isPermissionOwner()
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

        if (!stateMachine.isRunning()) {
            return ServiceStatus()
        }

        // TODO disable authorized apps when running unsupported version
        val unsupportedVersion =
            Shizuku.isPreV11() || (Shizuku.getVersion() == 11 && Shizuku.getServerPatchVersion() < 3)
        val uid = Shizuku.getUid()
        val apiVersion = Shizuku.getVersion()
        val patchVersion = Shizuku.getServerPatchVersion().let { if (it < 0) 0 else it }
        val permissionTest =
            Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED

        val isRunning = uid != -1 && shizukuStateMachine.isRunning()
        return ServiceStatus(uid, apiVersion, patchVersion, permissionTest, isRunning)
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
        if (!preferencesRepository.startOnBoot.get() && !preferencesRepository.watchdog.get()) return
        if (!powerManagerHelper.isIgnoringBatteryOptimizations()) {
            _events.trySend(HomeEvent.ShowBatteryOptimizationSnackbar)
        }
    }

}
