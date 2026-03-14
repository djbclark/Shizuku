package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.snackbar
import moe.shizuku.manager.databinding.HomeFragmentBinding
import moe.shizuku.manager.permission.ui.authorizedapps.AppsViewModel
import moe.shizuku.manager.shizukuservice.services.AdbPairingService
import moe.shizuku.manager.shizukuservice.ui.showAccessibilityDialog
import moe.shizuku.manager.updater.UpdateHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status

class HomeFragment : Fragment() {
    companion object {
        const val ARG_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
        const val ARG_START_SERVICE = "start_service"
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

        setupCards()

        requireActivity().addMenuProvider(
            HomeMenuProvider(this),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        val shouldShowAccessibilityPairingDialog =
            arguments?.getBoolean(ARG_SHOW_PAIRING_DIALOG, false) ?: false
        if (shouldShowAccessibilityPairingDialog) {
            showAccessibilityDialog(requireContext())
            arguments?.putBoolean(ARG_SHOW_PAIRING_DIALOG, false)
        }

        val shouldStartService = arguments?.getBoolean(ARG_START_SERVICE, false) ?: false
        if (shouldStartService) {
            val nm =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AdbPairingService.NOTIFICATION_ID)
        }

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
                    duration = Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.fix) {
                    val intent = PowerManagerHelper.getBatteryOptimizationIntent()
                    startActivity(intent)
                }.show()
            }
        }
        homeModel.checkBatteryOptimization()

        appsModel.grantedCount.observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                val grantedCount = it.data ?: 0
                binding.authorizedAppsCard.title = resources.getQuantityString(
                    R.plurals.authorized_apps_count,
                    grantedCount,
                    grantedCount,
                )
            }
        }

        lifecycleScope.launch {
            if (UpdateHelper.isCheckForUpdatesEnabled() && UpdateHelper.isNewUpdateAvailable()) {
                snackbar(
                    msg = getString(R.string.update_available),
                    duration = Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.update) {
                    lifecycleScope.launch {
                        UpdateHelper.update()
                    }
                }.show()
                UpdateHelper.updateLastPromptedVersion()
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

    private fun setupCards() {
        binding.apply {
            stealthCard.apply {
                title = getString(R.string.stealth_mode)
                icon = R.drawable.ic_visibility_off_outline_24
                onClickListener = {
                    findNavController().navigate(R.id.navigate_to_stealth)
                }
            }

            authorizedAppsCard.apply {
                title = getString(R.string.authorized_apps)
                icon = R.drawable.ic_settings_outline_24dp
                onClickListener = {
                    findNavController().navigate(R.id.navigate_to_authorized_apps)
                }
            }

            terminalCard.apply {
                title = getString(R.string.terminal_apps)
                icon = R.drawable.ic_terminal_24
                onClickListener = {
                    findNavController().navigate(R.id.navigate_to_terminal_apps)
                }
            }

            intentsCard.apply {
                title = getString(R.string.intents)
                icon = R.drawable.ic_integration_instructions_24
                onClickListener = {
                    findNavController().navigate(R.id.navigate_to_intents)
                }
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
