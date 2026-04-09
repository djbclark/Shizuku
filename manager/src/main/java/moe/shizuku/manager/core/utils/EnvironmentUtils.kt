package moe.shizuku.manager.core.utils

import android.content.Context
import com.topjohnwu.superuser.Shell

class EnvironmentUtils(
    private val context: Context
) {

    val packageName: String get() = context.packageName

    fun isPackageInstalled(pkgName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(pkgName, 0)
    }.isSuccess

    companion object {
        fun isRooted(): Boolean =
            Shell.isAppGrantedRoot() ?:
            Shell.getShell().isRoot
    }
}
