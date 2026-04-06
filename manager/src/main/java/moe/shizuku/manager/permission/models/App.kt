package moe.shizuku.manager.permission.models

import android.content.pm.ApplicationInfo
import moe.shizuku.manager.core.extensions.toUserId

data class App(
    val info: ApplicationInfo,
    val label: String
) {
    val uid = info.uid
    val userId = uid.toUserId
    val packageName: String = info.packageName
}