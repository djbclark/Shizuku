package moe.shizuku.manager.core.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.SystemProperties
import androidx.annotation.ChecksSdkIntAtLeast
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.StartMode

class EnvironmentUtils(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {

    val packageName: String get() = context.packageName

    fun isWatch(): Boolean {
        return (context.getSystemService(UiModeManager::class.java).currentModeType
                == Configuration.UI_MODE_TYPE_WATCH)
    }

    fun isTelevision(): Boolean {
        return (context.getSystemService(UiModeManager::class.java).currentModeType
                == Configuration.UI_MODE_TYPE_TELEVISION ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun hasWirelessDebugging(): Boolean = if (isTelevision())
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        else Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun isWifiRequired(): Boolean {
        return preferencesRepository.startMode.get() == StartMode.WADB
                && (getAdbTcpPort() <= 0 || !preferencesRepository.tcpMode.get())
    }

    fun getAdbTcpPort(): Int {
        var port = SystemProperties.getInt("service.adb.tcp.port", -1)
        if (port == -1) port = SystemProperties.getInt("persist.adb.tcp.port", -1)
        if (port == -1 && isTelevision() && !hasWirelessDebugging()) port =
            preferencesRepository.tcpPort.get()
        return port
    }

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
