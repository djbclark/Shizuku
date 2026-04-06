package moe.shizuku.manager.permission

import rikka.shizuku.Shizuku

class PermissionManager {

    companion object {
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
    }

    fun isGranted(uid: Int): Boolean =
        (Shizuku.getFlagsForUid(uid, MASK_PERMISSION) and FLAG_ALLOWED) == FLAG_ALLOWED

    fun setGranted(uid: Int, grant: Boolean) =
        if (grant) grant(uid) else revoke(uid)

    private fun grant(uid: Int) =
        Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, FLAG_ALLOWED)

    private fun revoke(uid: Int) =
        Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, 0)
}
