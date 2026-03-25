package moe.shizuku.manager.intents.receivers

import android.content.Context
import android.content.Intent
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.utils.ShizukuStateMachine
import org.koin.core.component.get
import rikka.shizuku.Shizuku

class ManualStopReceiver : AuthenticatedReceiver() {
    override fun onAuthenticated(context: Context, intent: Intent) {
        val applicationId = BuildConfig.APPLICATION_ID
        if (intent.action != "${applicationId}.STOP") return

        val shizukuStateMachine: ShizukuStateMachine = get()

        if (!shizukuStateMachine.isRunning()) return

        shizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
        runCatching { Shizuku.exit() }
    }
}