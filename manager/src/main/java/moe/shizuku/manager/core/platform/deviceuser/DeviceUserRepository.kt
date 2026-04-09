package moe.shizuku.manager.core.platform.deviceuser

import android.content.pm.UserInfo
import android.system.Os
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.toUserId
import moe.shizuku.manager.core.platform.userservice.UserServiceRepository

class DeviceUserRepository(
    private val userServiceRepository: UserServiceRepository
) {
    private var userCache: Map<Int, DeviceUser> = emptyMap()
    private val mutex = Mutex()

    private val currentUserId = Os.getuid().toUserId

    private fun isCurrentUser(userId: Int) = userId == currentUserId

    fun getCurrentUser(): DeviceUser =
        userCache[currentUserId] ?: createPlaceholder(currentUserId)

    suspend fun getUser(userId: Int): DeviceUser {
        if (userCache.isEmpty()) getUsers()
        return userCache[userId] ?: createPlaceholder(userId)
    }

    suspend fun getUsers(): Map<Int, DeviceUser> = mutex.withLock {
        if (userCache.isEmpty()) {
            runCatching {
                userServiceRepository.getService().getUsers().associate {
                    it.id to it.toDeviceUser()
                }.also { userCache = it }
            }.onFailure {
                Log.w(TAG, "getUsers", it)
            }
        }

        return@withLock userCache
    }

    private fun createPlaceholder(userId: Int) = DeviceUser(
        id = userId,
        name = "User $userId",
        isCurrentUser = isCurrentUser(userId),
    )

    private fun UserInfo.toDeviceUser() = DeviceUser(
        id = this.id,
        name = this.name,
        isCurrentUser = isCurrentUser(this.id),
    )
}