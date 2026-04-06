package moe.shizuku.manager.privilegedservice.api

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.BuildConfig
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

class UserServiceManager(context: Context) {
    private var userService: IUserService? = null

    private val args =
        Shizuku.UserServiceArgs(ComponentName(context, UserService::class.java))
            .daemon(false)
            .debuggable(BuildConfig.DEBUG)
            .processNameSuffix("userService")
            .tag("UserService")
            .version(BuildConfig.VERSION_CODE)

    suspend fun getService(): IUserService = withContext(Dispatchers.IO) {
        userService ?: withTimeout(5_000) {
            suspendCancellableCoroutine { cont ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                        if (binder == null || !binder.pingBinder()) return
                        val service = IUserService.Stub.asInterface(binder)
                        userService = service
                        if (cont.isActive) cont.resume(service)
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        userService = null
                    }
                }

                Shizuku.bindUserService(args, connection)
            }
        }
    }
}