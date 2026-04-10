package moe.shizuku.manager.core.platform.userservice

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.core.extensions.TAG
import rikka.shizuku.Shizuku

class UserServiceRepository(context: Context) {
    private val userService = MutableStateFlow<IUserService?>(null)
    private val mutex = Mutex()

    private val args =
        Shizuku.UserServiceArgs(ComponentName(context, UserService::class.java))
            .daemon(false)
            .debuggable(BuildConfig.DEBUG)
            .processNameSuffix("userService")
            .tag("UserService")
            .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            if (binder == null || !binder.isBinderAlive) return

            val service = IUserService.Stub.asInterface(binder)

            userService.value = service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            userService.value = null
        }
    }

    suspend fun getService(): IUserService = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = userService.value
            if (current != null && current.asBinder().isBinderAlive) {
                return@withLock current
            }

            Log.d(TAG, "bindUserService")
            Shizuku.bindUserService(args, connection)

            withTimeout(5_000) {
                userService.filterNotNull().first()
            }.also {
                Log.d(TAG, "Bound to UserService")
            }
        }
    }
}