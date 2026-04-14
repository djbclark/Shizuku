package moe.shizuku.manager.core.platform.services

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class KeyguardHelper(private val context: Context) {
    private val keyguardManager: KeyguardManager by systemService(context)

    val isKeyguardLocked: Boolean
        get() = keyguardManager.isKeyguardLocked

    suspend fun waitForUnlock(): Unit = suspendCancellableCoroutine { cont ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                context.unregisterReceiver(this)
                cont.resume(Unit)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        cont.invokeOnCancellation {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}