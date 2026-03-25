package moe.shizuku.manager.updater.data

import android.content.Context
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.updater.models.AppRelease
import moe.shizuku.manager.updater.models.Version
import java.io.File
import java.security.MessageDigest

class ReleaseRepository(
    private val context: Context,
    private val remoteDataSource: ReleaseRemoteDataSource
) {
    suspend fun getLatestRelease(channel: UpdateChannel): AppRelease {
        val releases = remoteDataSource.fetchReleases()
        val filteredReleases = if (channel == UpdateChannel.BETA) {
            releases
        } else {
            releases.filter { !it.prerelease }
        }

        return filteredReleases.mapNotNull { dto ->
            val version = Version.parse(dto.tag_name) ?: return@mapNotNull null
            val asset =
                dto.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@mapNotNull null

            AppRelease(
                version = version,
                filename = asset.name,
                url = asset.browser_download_url,
                digest = asset.digest
            )
        }.maxByOrNull { it.version } ?: throw Exception("No valid releases found")
    }

    suspend fun downloadRelease(release: AppRelease): File {
        val apkFile = File(context.cacheDir, release.filename)
        remoteDataSource.downloadFile(release.url, apkFile)

        val downloadedDigest = "sha256:" + apkFile.sha256()
        if (downloadedDigest != release.digest) {
            throw SecurityException("Digest of downloaded file does not match the one reported by GitHub")
        }

        return apkFile
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        inputStream().use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
