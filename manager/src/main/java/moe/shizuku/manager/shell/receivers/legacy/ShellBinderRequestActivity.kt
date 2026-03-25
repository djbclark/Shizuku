package moe.shizuku.manager.shell.receivers.legacy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import moe.shizuku.manager.shell.ShellBinderRequestHandler
import org.koin.android.ext.android.inject

class ShellBinderRequestActivity : AppCompatActivity() {
    private val shellBinderRequestHandler: ShellBinderRequestHandler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action != "rikka.shizuku.intent.action.REQUEST_BINDER") return

        shellBinderRequestHandler.handleRequest(this, intent)
        finish()
    }
}
