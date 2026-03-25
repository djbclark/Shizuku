package moe.shizuku.manager.shell.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.shell.ShellBinderRequestHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ShellBinderRequestReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "rikka.shizuku.intent.action.REQUEST_BINDER") return
        
        val handler: ShellBinderRequestHandler = get()
        handler.handleRequest(context, intent)
    }
}
