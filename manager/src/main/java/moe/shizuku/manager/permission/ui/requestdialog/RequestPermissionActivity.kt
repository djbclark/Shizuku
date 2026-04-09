package moe.shizuku.manager.permission.ui.requestdialog

import android.app.Dialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.ConfirmationDialogBinding
import moe.shizuku.manager.privilegedservice.data.ShizukuStateMachine
import org.koin.android.ext.android.inject
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME

class RequestPermissionActivity : AppCompatActivity() {
    private lateinit var dialog: Dialog
    private val binding by viewBinding(ConfirmationDialogBinding::inflate)
    private val shizukuStateMachine: ShizukuStateMachine by inject()

    private fun setResult(
        requestUid: Int,
        requestPid: Int,
        requestCode: Int,
        allowed: Boolean,
        onetime: Boolean,
    ) {
        val data = Bundle()
        data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed)
        data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime)
        try {
            Shizuku.dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data)
        } catch (e: Throwable) {
            Log.e("RequestPermissionActivity", "dispatchPermissionConfirmationResult", e)
        }
    }

    private fun checkSelfPermission(): Boolean {
        val permission =
            Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED
        if (permission) return true

        val icon = AppCompatResources.getDrawable(this, R.drawable.ic_system_icon)
        icon?.setTint(MaterialColors.getColor(this, android.R.attr.colorPrimary, 0))


        val dialog = MaterialAlertDialogBuilder(this).setIcon(icon)
            .setTitle("Shizuku: ${getString(R.string.status_adb_restricted)}").setMessage(
                getString(R.string.status_adb_restricted_message, "PLACEHOLDER"),
            ).setPositiveButton(android.R.string.ok, null).setOnDismissListener { finish() }
            .create()
        dialog.setOnShowListener {
            (it as AlertDialog).findViewById<TextView>(android.R.id.message)?.movementMethod =
                LinkMovementMethod.getInstance()
        }
        try {
            dialog.show()
        } catch (_: Throwable) {
        }
        return false
    }

    private fun waitForBinder(): Boolean = runBlocking {
        try {
            withTimeout(5000) {
                shizukuStateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }
            }
            true
        } catch (e: TimeoutCancellationException) {
            Log.e("RequestPermissionActivity", "Binder not received in 5s", e)
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!waitForBinder()) {
            finish()
            return
        }

        val uid = intent.getIntExtra("uid", -1)
        val pid = intent.getIntExtra("pid", -1)
        val requestCode = intent.getIntExtra("requestCode", -1)
        val ai = intent.getParcelableExtra<ApplicationInfo>("applicationInfo")
        if (uid == -1 || pid == -1 || ai == null) {
            finish()
            return
        }
        if (!checkSelfPermission()) {
            setResult(uid, pid, requestCode, allowed = false, onetime = true)
            return
        }

        val label = try {
            ai.loadLabel(packageManager)
        } catch (_: Exception) {
            ai.packageName
        }

        binding.apply {
            button1.setOnClickListener {
                setResult(uid, pid, requestCode, allowed = true, onetime = false)
                dialog.dismiss()
            }
            button3.setOnClickListener {
                setResult(uid, pid, requestCode, allowed = false, onetime = true)
                dialog.dismiss()
            }
            title.text = getString(
                R.string.permission_warning_template,
                label,
                getString(R.string.permission_group_description),
            )
        }

        dialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setCancelable(false)
            .setOnDismissListener { finish() }
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
}
