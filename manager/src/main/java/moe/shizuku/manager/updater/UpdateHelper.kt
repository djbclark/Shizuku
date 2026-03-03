package moe.shizuku.manager.updater

import android.util.Log
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuApplication
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.ui.components.toast
import moe.shizuku.manager.updater.data.ReleaseRepository
import moe.shizuku.manager.updater.models.AppRelease
import moe.shizuku.manager.core.utils.changePackageName
import moe.shizuku.manager.core.utils.getVersionName
import moe.shizuku.manager.core.utils.installPackage
import java.io.File

object UpdateHelper {
    private val app = ShizukuApplication.application
    private val appContext = ShizukuApplication.appContext
    private val repository = ReleaseRepository

    private lateinit var latestRelease: AppRelease

    suspend fun checkAndInstallUpdates() {
        if (isUpdateAvailable()) {
            update()
        } else {
            appContext.toast(R.string.update_latest_installed)
        }
    }

    fun isCheckForUpdatesEnabled(): Boolean = PreferencesRepository.getCheckForUpdates()

    suspend fun isNewUpdateAvailable(): Boolean {
        val lastPromptedVersionStr = repository.getLastPromptedVersion()
        val lastPromptedVersion = moe.shizuku.manager.updater.models.Version.parse(lastPromptedVersionStr ?: "")
            ?: moe.shizuku.manager.updater.models.Version.parse(getVersionName())
            ?: return false
            
        return if (isUpdateAvailable()) latestRelease.version > lastPromptedVersion else false
    }

    suspend fun isUpdateAvailable(): Boolean {
        return try {
            val channel = PreferencesRepository.getUpdateChannel()
            val latest = repository.getLatestRelease(channel)
            latestRelease = latest
            val current = moe.shizuku.manager.updater.models.Version.parse(getVersionName()) ?: return false
            latest.version > current
        } catch (e: Exception) {
            Log.e("UpdateHelper", "Update check failed", e)
            appContext.toast(R.string.update_check_failed)
            false
        }
    }

    fun updateLastPromptedVersion() {
        if (::latestRelease.isInitialized) {
            repository.setLastPromptedVersion(latestRelease.version.toString())
        }
    }

    suspend fun update() {
        if (!::latestRelease.isInitialized && !isUpdateAvailable()) return

        appContext.toast(R.string.update_downloading)

        try {
            val downloadedFile = repository.downloadRelease(latestRelease)
            val apk = processDownloadedApk(downloadedFile)
            
            if (apk == null) {
                appContext.toast(R.string.update_download_failed)
                return
            }

            appContext.installPackage(apk) { isSuccess, _ ->
                val toastMsg = if (isSuccess) appContext.getString(R.string.update_success)
                else appContext.getString(R.string.update_failed)
                appContext.toast(toastMsg)
            }
        } catch (e: Exception) {
            Log.e("UpdateHelper", "Update failed", e)
            appContext.toast(R.string.update_failed)
        }
    }

    private fun processDownloadedApk(file: File): File? {
        val pm = appContext.packageManager
        val apkPackageName = pm.getPackageArchiveInfo(file.path, 0)?.packageName
        
        if (app.packageName != apkPackageName) {
            return try {
                Log.d("UpdateHelper", "Changing package name from $apkPackageName to ${app.packageName}")
                file.changePackageName(app.packageName)
            } catch (e: Exception) {
                null
            }
        }
        return file
    }
}
