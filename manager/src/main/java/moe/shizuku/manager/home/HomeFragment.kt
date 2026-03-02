package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbPairingService
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.ui.components.snackbar
import moe.shizuku.manager.databinding.HomeFragmentBinding
import moe.shizuku.manager.permission.ui.authorizedapps.AppsViewModel
import moe.shizuku.manager.shizukuservice.ui.showAccessibilityDialog
import moe.shizuku.manager.updater.UpdateHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status

class HomeFragment : Fragment() {
    companion object {
        const val EXTRA_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
        const val EXTRA_START_SERVICE_VIA_WADB = "start_service_via_wadb"
    }

    private val homeModel: HomeViewModel by viewModels()
    private val appsModel: AppsViewModel by viewModels()

    private lateinit var binding: HomeFragmentBinding

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isRunning()) {
            homeModel.reload()
            appsModel.load()
        } else if (ShizukuStateMachine.isDead()) {
            homeModel.reload()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        requireActivity().addMenuProvider(
            HomeMenuProvider(this),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        homeModel.serviceStatus.observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                val status = homeModel.serviceStatus.value?.data ?: return@observe
                binding.statusCard.update(status)
            }
        }

        homeModel.shouldShowRebootDialog.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                showExitDialog(
                    getString(R.string.home_reboot_required),
                    getString(R.string.home_reboot_required_message),
                )
            }
        }

        homeModel.shouldShowUninstallDialog.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                showExitDialog(
                    getString(R.string.home_duplicate_app_detected),
                    getString(R.string.home_duplicate_app_detected_message),
                )
            }
        }

        homeModel.shouldShowBatteryOptimizationSnackbar.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                snackbar(
                    msg = getString(R.string.home_battery_optimization),
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = getString(R.string.fix),
                    action = {
                        val intent = PowerManagerHelper.getBatteryOptimizationIntent()
                        startActivity(intent)
                    }
                )
            }
        }
        homeModel.checkBatteryOptimization()

        appsModel.grantedCount.observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                // binding.authorizedAppsCard.update(it.data ?: 0)
            }
        }

        lifecycleScope.launch {
            if (UpdateHelper.isCheckForUpdatesEnabled() && UpdateHelper.isNewUpdateAvailable()) {
                snackbar(
                    msg = getString(R.string.update_available),
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = getString(R.string.update),
                    action = {
                        lifecycleScope.launch {
                            UpdateHelper.update()
                        }
                    }
                )
                UpdateHelper.updateLastPromptedVersion()
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun onNewIntent(intent: Intent?) {
        intent?.let {
            val showDialog = it.getBooleanExtra(EXTRA_SHOW_PAIRING_DIALOG, false)
            if (showDialog) showAccessibilityDialog(requireContext())

            val startWadb = it.getBooleanExtra(EXTRA_START_SERVICE_VIA_WADB, false)
            if (startWadb) {
                val nm =
                    requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(AdbPairingService.NOTIFICATION_ID)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeModel.reload()
        appsModel.load()
    }

    private fun showExitDialog(
        title: String,
        message: String,
    ) {
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.exit, null)
                .setOnDismissListener {
                    requireActivity().finishAffinity()
                }.create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    override fun onDestroyView() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroyView()
    }
}