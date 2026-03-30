package moe.shizuku.manager.intents.receivers

import android.content.Context
import android.content.Intent
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.privilegedservice.ShizukuReceiverStarter
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ManualStartReceiver : AuthenticatedReceiver(), KoinComponent {
    override fun onAuthenticated(context: Context, intent: Intent) {
        val applicationId = BuildConfig.APPLICATION_ID
        if (intent.action != "${applicationId}.START") return

        val shizukuReceiverStarter: ShizukuReceiverStarter = get()
        shizukuReceiverStarter.start()
    }
}
