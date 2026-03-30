package moe.shizuku.manager.intents.receivers

import android.content.Context
import android.content.Intent
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import org.koin.core.component.get

class ManualStopReceiver : AuthenticatedReceiver() {
    override fun onAuthenticated(context: Context, intent: Intent) {
        val applicationId = BuildConfig.APPLICATION_ID
        if (intent.action != "${applicationId}.STOP") return

        get<PrivilegedServiceManager>().stopService()
    }
}