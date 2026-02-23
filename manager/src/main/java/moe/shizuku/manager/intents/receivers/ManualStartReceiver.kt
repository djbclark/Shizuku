package moe.shizuku.manager.receiver

import android.content.Context
import android.content.Intent
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.core.android.receivers.AuthenticatedReceiver

class ManualStartReceiver : AuthenticatedReceiver() {
    override fun onAuthenticated(context: Context, intent: Intent) {
        val applicationId = BuildConfig.APPLICATION_ID
        if (intent.action != "${applicationId}.START") return

        ShizukuReceiverStarter.start(context)
    }
}