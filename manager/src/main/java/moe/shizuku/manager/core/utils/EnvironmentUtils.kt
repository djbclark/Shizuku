package moe.shizuku.manager.core.utils

import android.content.Context
import com.topjohnwu.superuser.Shell

class EnvironmentUtils(
    private val context: Context
) {

    val packageName: String get() = context.packageName

    fun isPermissionOwner(
        permissionGroup: String,
        permission: String
    ): Result<Boolean> = runCatching {
        context.packageManager.getPermissionGroupInfo(permissionGroup, 0)
        val info = context.packageManager.getPermissionInfo(permission, 0)
        info.packageName == context.packageName
    }

    fun isPackageInstalled(pkgName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(pkgName, 0)
    }.isSuccess

    companion object {
        fun isRooted(): Boolean {
            return Shell.getShell().isRoot
        }
    }
}
