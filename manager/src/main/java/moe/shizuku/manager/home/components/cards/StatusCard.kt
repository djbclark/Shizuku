package moe.shizuku.manager.home.components.cards

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.databinding.HomeStatusCardBinding
import moe.shizuku.manager.shizukuservice.models.ServiceStatus
import moe.shizuku.manager.shizukuservice.ui.AdbPairDialogFragment
import moe.shizuku.manager.shizukuservice.ui.showAccessibilityDialog
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants

class StatusCard
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val binding =
        HomeStatusCardBinding.inflate(
            LayoutInflater.from(context),
            this,
            true,
        )

    private val cardTitle: String
        get() =
            context.getString(
                R.string.status_stopped
            )
    private val cardIcon: Int
        get() = R.drawable.ic_server_error_24dp

    init {
        setTitle(cardTitle)
        setIcon(cardIcon)

        binding.buttonStart.setOnClickListener {
            // TODO implement start logic
        }

        if (EnvironmentUtils.isTlsSupported()) {
            binding.buttonPair.setOnClickListener {
                onPairClicked(context)
            }
        } else {
            binding.buttonPair.visibility = GONE
        }
    }

    private fun setTitle(text: String) {
        binding.title.text = text
    }

    private fun setIcon(resId: Int) {
        binding.icon.setImageResource(resId)
    }

    fun update(status: ServiceStatus) {
        val ok = status.isRunning
        val isRoot = status.uid == 0
        val apiVersion = status.apiVersion
        val patchVersion = status.patchVersion
        if (ok) {
            setIcon(R.drawable.ic_server_ok_24dp)
        } else {
            setIcon(R.drawable.ic_server_error_24dp)
        }
        val user = if (isRoot) "root" else "adb"
        val title =
            if (ok) {
                context.getString(R.string.status_running)
            } else {
                context.getString(R.string.status_stopped)
            }
        val versionStr =
            context.getString(
                R.string.status_version,
                "$apiVersion.$patchVersion",
                user,
            )
        val updateStr =
            context.getString(
                R.string.status_version_update,
                "${Shizuku.getLatestServiceVersion()}.${ShizukuApiConstants.SERVER_PATCH_VERSION}",
            )
        val summary =
            if (ok) {
                if (apiVersion != Shizuku.getLatestServiceVersion() ||
                    status.patchVersion != ShizukuApiConstants.SERVER_PATCH_VERSION
                ) {
                    "$versionStr. $updateStr"
                } else {
                    versionStr
                }
            } else {
                ""
            }
        binding.title.text = title
        binding.summary.text = summary
    }

//    private fun stopButton() {
//        if (ShizukuStateMachine.isRunning()) {
//            MaterialAlertDialogBuilder(context)
//                .setMessage(R.string.stop_dialog_message)
//                .setPositiveButton(android.R.string.ok) { _, _ ->
//                    ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
//                    runCatching { Shizuku.exit() }
//                }.setNegativeButton(android.R.string.cancel, null)
//                .show()
//        }
//    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onPairClicked(context: Context) {
        if (EnvironmentUtils.isTelevision()) {
            showAccessibilityDialog(context)
        } else if (PreferencesRepository.legacyPairing.value) {
            (context as? FragmentActivity)?.supportFragmentManager?.let {
                AdbPairDialogFragment().show(it)
            }
        } else {
            findNavController().navigate(R.id.navigate_to_pairing)
        }
    }

}
