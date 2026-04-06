package moe.shizuku.manager.permission.data

import android.content.Context
import android.content.pm.PackageInfo
import moe.shizuku.manager.core.android.DeviceUserHelper
import moe.shizuku.manager.permission.models.App
import moe.shizuku.manager.privilegedservice.api.UserServiceManager

class AuthorizedAppsRepository(
    private val context: Context,
    private val deviceUserHelper: DeviceUserHelper,
    private val userServiceManager: UserServiceManager
) {

    suspend fun getAppsDeclaringPermission(allUsers: Boolean = true): List<App> {
        val appsList = if (allUsers) {
            getInstalledAppsForAllUsers()
        } else {
            getInstalledAppsForUser(deviceUserHelper.myUserId)
        }

        return appsList.map {
            val appInfo = it.applicationInfo!!
            App(
                info = appInfo,
                label = context.packageManager.getApplicationLabel(appInfo).toString()
            )
        }
    }

    private suspend fun getInstalledAppsForAllUsers(): MutableList<PackageInfo> {
        val users = deviceUserHelper.getUsers().keys
        val appsList = mutableListOf<PackageInfo>()

        for (user in users) {
            val userApps = getInstalledAppsForUser(user)
            appsList.addAll(userApps)
        }

        return appsList
    }

    private suspend fun getInstalledAppsForUser(userId: Int) =
        userServiceManager.getService()
            .getInstalledPackagesAsUser(userId)

}