package moe.shizuku.manager.updater

import android.content.Context
import android.util.Log
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.preferences.data.string
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.utils.ApkUtils
import moe.shizuku.manager.updater.data.ReleaseRepository
import moe.shizuku.manager.updater.models.AppRelease
import moe.shizuku.manager.updater.models.Version
import java.io.File

class UpdateHelper(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val repository: ReleaseRepository,
    private val apkUtils: ApkUtils
) {
    private lateinit var latestRelease: AppRelease
    private val lastPromptedVersion by preferencesRepository.pref {
        string("last_prompted_version")
    }

    suspend fun checkAndInstallUpdates() {
        if (isUpdateAvailable()) {
            update()
        } else {
            context.toast(R.string.update_latest_installed)
        }
    }

    fun isCheckForUpdatesEnabled(): Boolean = preferencesRepository.checkForUpdates.get()

    suspend fun isNewUpdateAvailable(): Boolean {
        val lastPromptedVersionStr = lastPromptedVersion.get()
        val lastPromptedVersion =
            Version.parse(lastPromptedVersionStr ?: "")
                ?: Version.parse(apkUtils.getVersionName())
                ?: return false

        return if (isUpdateAvailable()) latestRelease.version > lastPromptedVersion else false
    }

    private suspend fun isUpdateAvailable(): Boolean {
        return try {
            val channel = preferencesRepository.updateChannel.get()
            val latest = repository.getLatestRelease(channel)
            latestRelease = latest
            val current =
                Version.parse(apkUtils.getVersionName()) ?: return false
            latest.version > current
        } catch (e: Exception) {
            Log.e("UpdateHelper", "Update check failed", e)
            context.toast(R.string.update_check_failed)
            false
        }
    }

    fun updateLastPromptedVersion() {
        if (::latestRelease.isInitialized) {
            lastPromptedVersion.set(latestRelease.version.toString())
        }
    }

    suspend fun update() {
        if (!::latestRelease.isInitialized && !isUpdateAvailable()) return

        context.toast(R.string.update_downloading)

        try {
            val downloadedFile = repository.downloadRelease(latestRelease)
            val apk = processDownloadedApk(downloadedFile)

            if (apk == null) {
                context.toast(R.string.update_download_failed)
                return
            }

            apkUtils.installPackage(apk) { isSuccess, _ ->
                val toastMsg = if (isSuccess) context.getString(R.string.update_success)
                else context.getString(R.string.update_failed)
                context.toast(toastMsg)
            }
        } catch (e: Exception) {
            Log.e("UpdateHelper", "Update failed", e)
            context.toast(R.string.update_failed)
        }
    }

    private fun processDownloadedApk(file: File): File? {
        val pm = context.packageManager
        val apkPackageName = pm.getPackageArchiveInfo(file.path, 0)?.packageName

        if (context.packageName != apkPackageName) {
            return try {
                Log.d(
                    "UpdateHelper",
                    "Changing package name from $apkPackageName to ${context.packageName}"
                )
                apkUtils.changePackageName(file, context.packageName)
            } catch (_: Exception) {
                null
            }
        }
        return file
    }
}
