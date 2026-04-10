package moe.shizuku.manager.permission.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import moe.shizuku.manager.core.extensions.getAppLabel
import moe.shizuku.manager.core.platform.device.user.DeviceUserRepository
import moe.shizuku.manager.core.platform.userservice.UserServiceRepository
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.models.AuthorizedAppsItem

class AuthorizedAppsRepository(
    private val context: Context,
    private val deviceUserRepository: DeviceUserRepository,
    private val userServiceRepository: UserServiceRepository,
    private val permissionManager: PermissionManager
) {
    // StateFlow so we can cache the app list across screens, as it's an expensive operation
    private val _appsList = MutableStateFlow<List<AuthorizedAppsItem.App>?>(null)
    val appsList: SharedFlow<List<AuthorizedAppsItem.App>?> = _appsList.asStateFlow()
        .onSubscription {
            if (_appsList.value == null) {
                refresh()
            }
        }

    val grantedCount: Flow<Int> = _appsList.asStateFlow().filterNotNull().map { list ->
        list.count { it.isGranted }
    }.distinctUntilChanged()

    suspend fun refresh(allUsers: Boolean = true) {
        val users = if (allUsers) {
            deviceUserRepository.getUsers()
        } else {
            listOf(deviceUserRepository.getCurrentUser())
        }

        _appsList.value = users.flatMap { user ->
            userServiceRepository.getService().getInstalledApplicationsAsUser(user.id)
                .map { appInfo ->
                    AuthorizedAppsItem.App(
                        appInfo = appInfo,
                        isGranted = runCatching {
                            permissionManager.isGranted(appInfo.uid)
                        }.getOrDefault(false),
                        user = user,
                        label = context.getAppLabel(appInfo)
                    )
                }
        }.sortedBy { it.displayName }
    }

    fun updatePermission(app: AuthorizedAppsItem.App, granted: Boolean): Result<Unit> = runCatching {
        permissionManager.setGranted(app.uid, granted)
    }.onSuccess {
        _appsList.update { currentList ->
            currentList?.map {
                if (it == app) {
                    it.copy(isGranted = granted)
                } else it
            }
        }
    }

}