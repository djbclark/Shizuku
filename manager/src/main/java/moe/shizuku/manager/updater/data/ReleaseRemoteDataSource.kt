package moe.shizuku.manager.updater.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Serializable
data class GitHubReleaseDto(
    val tag_name: String,
    val prerelease: Boolean,
    val assets: List<GitHubAssetDto>
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    val browser_download_url: String,
    val digest: String
)

class ReleaseRemoteDataSource {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchReleases(): List<GitHubReleaseDto> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/thedjchi/Shizuku/releases".toHttpUrl()
        val request = Request(url)
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Couldn't fetch releases: ${response.code}")
        val body = response.body.string()

        json.decodeFromString<List<GitHubReleaseDto>>(body)
    }

    suspend fun downloadFile(url: String, targetFile: File): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

        response.body.byteStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
