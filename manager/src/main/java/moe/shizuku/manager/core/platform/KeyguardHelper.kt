package moe.shizuku.manager.core.platform

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class KeyguardHelper(private val context: Context) {
    val isKeyguardLocked: Boolean
        get() {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return km.isKeyguardLocked
        }

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