package moe.shizuku.manager.core.android

import android.system.Os
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.toUserId
import moe.shizuku.manager.privilegedservice.api.UserServiceManager

class DeviceUserHelper(
    private val userServiceManager: UserServiceManager
) {
    private var userCache: Map<Int, String> = emptyMap()
    private val mutex = Mutex()

    val myUserId
        get() = Os.getuid().toUserId

    suspend fun getUserName(userId: Int): String {
        if (userCache.isEmpty()) getUsers()
        return userCache[userId] ?: "User $userId"
    }

    suspend fun getUsers(): Map<Int, String> = mutex.withLock {
        if (userCache.isEmpty()) {
            runCatching {
                userServiceManager.getService().getUsers().associate {
                    it.id to it.name
                }.also { userCache = it }
            }.onFailure {
                Log.w(TAG, "getUsers", it)
            }
        }

        return@withLock userCache
    }
}
