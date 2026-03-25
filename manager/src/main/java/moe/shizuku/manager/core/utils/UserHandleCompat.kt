package moe.shizuku.manager.core.utils

import android.system.Os

class UserHandleCompat {
    fun getUserId(uid: Int): Int {
        return uid / 100000
    }

    fun myUserId() = getUserId(Os.getuid())
}
