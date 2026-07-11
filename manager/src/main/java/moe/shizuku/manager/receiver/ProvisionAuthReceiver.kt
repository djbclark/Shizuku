package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Process
import android.widget.Toast
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings

/**
 * Allows ADB shell (or root) to seed the auth token so that fleet provisioning
 * scripts can set a known token once, then drive Shizuku start/stop via the
 * existing [ManualStartReceiver]/[ManualStopReceiver] without opening the UI.
 *
 * Example:
 *   adb shell am broadcast -a moe.shizuku.manager.PROVISION_AUTH \
 *       -e auth_token "YOUR_TOKEN"
 */
class ProvisionAuthReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "${context.packageName}.PROVISION_AUTH") return

        val callingUid = Binder.getCallingUid()
        if (callingUid != Process.SHELL_UID && callingUid != Process.ROOT_UID) {
            Toast.makeText(
                context,
                R.string.notification_auth_invalid_title,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val token = intent.getStringExtra("auth_token")
        if (token.isNullOrEmpty()) {
            Toast.makeText(
                context,
                R.string.notification_auth_missing_title,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        ShizukuSettings.setAuthToken(token)
        Toast.makeText(
            context,
            context.getString(R.string.home_automation_regenerate_token),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
