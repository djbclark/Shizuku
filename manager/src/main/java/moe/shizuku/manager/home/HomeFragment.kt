package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbPairingService
import moe.shizuku.manager.app.SnackbarHelper
import moe.shizuku.manager.core.android.settings.SettingsHelper
import moe.shizuku.manager.databinding.HomeActivityBinding
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.shizukuservice.ui.showAccessibilityDialog
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.UpdateHelper
import rikka.lifecycle.Status

class HomeFragment : Fragment() {
    companion object {
        const val EXTRA_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
        const val EXTRA_START_SERVICE_VIA_WADB = "start_service_via_wadb"
    }

    private val homeModel: HomeViewModel by viewModels()
    private val appsModel: AppsViewModel by viewModels()

    private lateinit var binding: HomeActivityBinding

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
        binding = HomeActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                SnackbarHelper.show(
                    requireActivity(),
                    binding.root,
                    msg = getString(R.string.home_battery_optimization),
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = getString(R.string.fix),
                    action = {
                        SettingsHelper.requestIgnoreBatteryOptimizations(
                            requireContext(),
                            null
                        )
                    },
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
                SnackbarHelper.show(
                    requireActivity(),
                    binding.root,
                    msg = getString(R.string.update_available),
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = getString(R.string.update),
                    action = {
                        lifecycleScope.launch {
                            UpdateHelper.update()
                        }
                    },
                )
                UpdateHelper.updateLastPromptedVersion()
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

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

    override fun onPause() {
        super.onPause()
        SnackbarHelper.dismiss()
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