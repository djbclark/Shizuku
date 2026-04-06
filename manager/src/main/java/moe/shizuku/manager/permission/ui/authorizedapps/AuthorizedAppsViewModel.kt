package moe.shizuku.manager.permission.ui.authorizedapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.DeviceUserHelper
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.data.AuthorizedAppsRepository
import moe.shizuku.manager.permission.models.AuthorizedAppsEvent
import moe.shizuku.manager.permission.models.AuthorizedAppsItem
import moe.shizuku.manager.permission.models.AuthorizedAppsUiState
import rikka.shizuku.Shizuku

class AuthorizedAppsViewModel(
    private val authorizedAppsRepository: AuthorizedAppsRepository,
    private val permissionManager: PermissionManager,
    private val deviceUserHelper: DeviceUserHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthorizedAppsUiState())
    val uiState: StateFlow<AuthorizedAppsUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthorizedAppsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState.value = AuthorizedAppsUiState(apps = getAppList())
        }
    }

    private suspend fun getAppList(): List<AuthorizedAppsItem.App> = withContext(Dispatchers.IO) {
        val appList = authorizedAppsRepository.getAppsDeclaringPermission()
        val myUserId = deviceUserHelper.myUserId

        appList.map { app ->
            val displayName = buildString {
                append(app.label)
                if (app.userId != myUserId) {
                    append(" - ")
                    append(deviceUserHelper.getUserName(app.userId))
                }
            }

            AuthorizedAppsItem.App(
                appInfo = app.info,
                displayName = displayName,
                packageName = app.packageName,
                isGranted = runCatching {
                    permissionManager.isGranted(app.uid)
                }.getOrDefault(false)
            )
        }.sortedBy { it.displayName }
    }

    fun toggleApp(item: AuthorizedAppsItem.App) =
        viewModelScope.launch(Dispatchers.IO) {
            item.setGranted(!item.isGranted)
                .onSuccess {
                    val updatedApps = _uiState.value.apps.map { app ->
                        if (app == item) app.copy(isGranted = !item.isGranted)
                        else app
                    }

                    _uiState.value = _uiState.value.copy(apps = updatedApps)
                }
                .onFailure {
                    if (it is SecurityException) handleSecurityException()
                    else _events.trySend(
                        AuthorizedAppsEvent.ShowError(R.string.authorized_apps_error_toggle)
                    )
                }
        }

    fun toggleAll(grant: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            var anyFailed = false
            var securityExceptionOccurred = false

            val updatedApps = mutableListOf<AuthorizedAppsItem.App>()

            for (app in uiState.value.apps) {
                if (securityExceptionOccurred || app.isGranted == grant) {
                    updatedApps.add(app)
                    continue
                }

                app.setGranted(grant).onSuccess {
                    updatedApps.add(app.copy(isGranted = grant))
                }.onFailure {
                    if (it is SecurityException) {
                        securityExceptionOccurred = true
                    } else {
                        anyFailed = true
                    }
                    updatedApps.add(app)
                }
            }

            _uiState.value = _uiState.value.copy(apps = updatedApps)

            if (securityExceptionOccurred) {
                handleSecurityException()
            } else if (anyFailed) {
                _events.trySend(AuthorizedAppsEvent.ShowError(R.string.authorized_apps_error_toggle_all))
            }
        }

    private fun AuthorizedAppsItem.App.setGranted(grant: Boolean) = runCatching {
        permissionManager.setGranted(appInfo.uid, grant)
    }

    private fun handleSecurityException() {
        val uid = runCatching {
            Shizuku.getUid()
        }.getOrDefault(-1)
        if (uid > 0) {
            _events.trySend(AuthorizedAppsEvent.NotifyAdbRestricted)
        }
    }
}
