package moe.shizuku.manager.core.platform.services.pkg

import android.content.Context

class PackageManagerHelper(private val context: Context) {
    private val packageManager by lazy {
        context.packageManager
    }

    val packageName: String by lazy {
        context.packageName
    }

    fun isPackageInstalled(pkgName: String): Boolean = runCatching {
        packageManager.getPackageInfo(pkgName, 0)
    }.isSuccess
}