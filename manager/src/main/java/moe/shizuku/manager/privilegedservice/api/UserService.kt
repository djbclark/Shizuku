package moe.shizuku.manager.privilegedservice.api

import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.UserInfo
import android.os.Build
import android.os.IUserManager
import android.os.ServiceManager
import androidx.annotation.Keep
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

    override fun getUsers(): List<UserInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            userManager.getUsers(true, true, true)
        } else {
            userManager.getUsers(true)
        }

    private val packageManager by lazy {
        IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    }

    override fun getInstalledPackagesAsUser(userId: Int): List<PackageInfo> =
        packageManager.getInstalledPackagesAsUser(0, userId)
            ?: emptyList()
}
