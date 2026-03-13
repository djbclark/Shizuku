package moe.shizuku.manager.updater.models

data class AppRelease(
    val version: Version,
    val filename: String,
    val url: String,
    val digest: String
)
