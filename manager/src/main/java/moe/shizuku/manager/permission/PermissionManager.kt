package moe.shizuku.manager.permission

import android.content.Context
import rikka.shizuku.Shizuku

class PermissionManager(
    private val context: Context
) {
    fun isGranted(uid: Int): Boolean =
        (Shizuku.getFlagsForUid(uid, MASK_PERMISSION) and FLAG_ALLOWED) == FLAG_ALLOWED

    fun setGranted(uid: Int, grant: Boolean): Unit =
        if (grant) grant(uid) else revoke(uid)

    private fun grant(uid: Int) =
        Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, FLAG_ALLOWED)

    private fun revoke(uid: Int) =
        Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, 0)

    fun isPermissionOwner(): Result<Boolean> = runCatching {
        context.packageManager.getPermissionGroupInfo(PERMISSION_GROUP, 0)
        val info = context.packageManager.getPermissionInfo(PERMISSION, 0)
        info.packageName == context.packageName
    }

    companion object {
        private const val PERMISSION_GROUP = "moe.shizuku.manager.permission-group.API"
        private const val PERMISSION = "moe.shizuku.manager.permission.API_V23"
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
    }
}
