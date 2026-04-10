package moe.shizuku.manager.core.platform.userservice

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManagerHidden
import android.content.pm.UserInfo
import android.os.IUserManager
import android.os.ServiceManager
import android.util.Log
import androidx.annotation.Keep
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.unsafeCast
import moe.shizuku.manager.core.platform.device.AndroidVersion
import kotlin.system.exitProcess

class UserService : IUserService.Stub {
    @Suppress("unused")
    constructor()

    private var context: Context? = null

    @Suppress("unused")
    @Keep
    constructor(context: Context) {
        this.context = context
    }

    override fun destroy() {
        context = null
        exitProcess(0)
    }

    override fun exit() {
        destroy()
    }

    private val userManager by lazy {
        IUserManager.Stub.asInterface(ServiceManager.getService("user"))
    }

    override fun getUsers(): List<UserInfo> = runCatching {
        if (AndroidVersion.isAtLeast11) {
            userManager.getUsers(true, true, true)
        } else {
            userManager.getUsers(true)
        }
    }.recoverCatching {
        userManager.getUsers(true)
    }.onFailure {
        Log.e(TAG, "getUsers", it)
    }.getOrDefault(emptyList())

    override fun getInstalledApplicationsAsUser(userId: Int): List<ApplicationInfo> = runCatching {
        val pm = unsafeCast<PackageManagerHidden>(context!!.packageManager)
        return pm.getInstalledApplicationsAsUser(0, userId)
    }.onFailure {
        Log.e(TAG, "getInstalledApplicationsAsUser", it)
    }.getOrDefault(emptyList())
}
