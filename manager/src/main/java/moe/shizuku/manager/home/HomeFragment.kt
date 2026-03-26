package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.View.GONE
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.android.settings.SystemSettingsPage
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import moe.shizuku.manager.core.extensions.openUrl
import moe.shizuku.manager.core.extensions.snackbar
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionViewModel
import moe.shizuku.manager.core.utils.AppIconCache
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.databinding.HomeFragmentBinding
import moe.shizuku.manager.databinding.HomeSimpleCardBinding
import moe.shizuku.manager.databinding.HomeStatusCardBinding
import moe.shizuku.manager.home.models.HomeEvent
import moe.shizuku.manager.permission.ui.authorizedapps.AuthorizedAppsViewModel
import moe.shizuku.manager.shizukuservice.models.ServiceStatus
import moe.shizuku.manager.shizukuservice.services.AdbPairingService
import moe.shizuku.manager.shizukuservice.starter.AdbStarter
import moe.shizuku.manager.shizukuservice.ui.showAccessibilityDialog
import moe.shizuku.manager.updater.UpdateHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.lifecycle.Status
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants

class HomeFragment : Fragment(R.layout.home_fragment) {
    companion object {
        const val ARG_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
        const val ARG_START_SERVICE = "start_service"
    }

    private val homeModel: HomeViewModel by viewModel()
    private val listSelectionModel: ListSelectionViewModel by viewModel()
    private val appsModel: AuthorizedAppsViewModel by viewModel()
    private val appIconCache: AppIconCache by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    private val environmentUtils: EnvironmentUtils by inject()
    private val powerManagerHelper: PowerManagerHelper by inject()
    private val updateHelper: UpdateHelper by inject()
    private val stateMachine: ShizukuStateMachine by inject()
    private val adbStarter: AdbStarter by inject()

    private val binding by viewBinding(HomeFragmentBinding::bind)

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (stateMachine.isRunning()) {
            homeModel.reload()
            appsModel.load()
        } else if (stateMachine.isDead()) {
            homeModel.reload()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        setupCards()

        requireActivity().addMenuProvider(
            HomeMenuProvider(this, updateHelper, appIconCache),
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
                launch {
                    listSelectionModel.results.collect {
                        homeModel.handleSelectionResult(it)
                    }
                }
            }
        }

        homeModel.checkBatteryOptimization()

        appsModel.grantedCount.observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                val grantedCount = it.data ?: 0
                binding.authorizedAppsCard.title.text = resources.getQuantityString(
                    R.plurals.authorized_apps_count,
                    grantedCount,
                    grantedCount,
                )
            }
        }

        lifecycleScope.launch {
            if (updateHelper.isCheckForUpdatesEnabled() && updateHelper.isNewUpdateAvailable()) {
                snackbar(
                    msg = getString(R.string.update_available),
                    duration = Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.update) {
                    lifecycleScope.launch {
                        updateHelper.update()
                    }
                }.show()
                updateHelper.updateLastPromptedVersion()
            }
        }

        stateMachine.addListener(stateListener)
    }

    private fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OpenUrl -> openUrl(event.url)

            HomeEvent.ShowRebootDialog -> {
                showExitDialog(
                    getString(R.string.home_reboot_required),
                    getString(R.string.home_reboot_required_message),
                )
            }

            HomeEvent.ShowUninstallDialog -> {
                showExitDialog(
                    getString(R.string.home_duplicate_app_detected),
                    getString(R.string.home_duplicate_app_detected_message),
                )
            }

            HomeEvent.ShowBatteryOptimizationSnackbar -> {
                snackbar(
                    msg = getString(R.string.home_battery_optimization),
                    duration = Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.fix) {
                    val intent = powerManagerHelper.getBatteryOptimizationIntent()
                    startActivity(intent)
                }.show()
            }
        }
    }

    private fun setupCards() = with(binding) {
        statusCard.apply {
            buttonStart.setOnClickListener {
                start()
            }

            buttonStop.setOnClickListener {
                stop()
            }

            if (environmentUtils.isTlsSupported() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                buttonPair.setOnClickListener {
                    onPairClicked(requireContext())
                }
            } else {
                buttonPair.visibility = GONE
            }
        }

        stealthCard.setup(
            title = getString(R.string.stealth_mode),
            icon = R.drawable.ic_visibility_off_outline_24,
            onClick = {
                findNavController().navigate(R.id.navigate_to_stealth)
            }
        )

        authorizedAppsCard.setup(
            title = getString(R.string.authorized_apps),
            icon = R.drawable.ic_settings_outline_24dp,
            onClick = {
                findNavController().navigate(R.id.navigate_to_authorized_apps)
            }
        )

        terminalCard.setup(
            title = getString(R.string.terminal_apps),
            icon = R.drawable.ic_terminal_24,
            onClick = {
                findNavController().navigate(R.id.navigate_to_terminal_apps)
            }
        )

        intentsCard.setup(
            title = getString(R.string.intents),
            icon = R.drawable.ic_integration_instructions_24,
            onClick = {
                findNavController().navigate(R.id.navigate_to_intents)
            }
        )
    }

    private fun HomeStatusCardBinding.update(status: ServiceStatus) {
        val ok = status.isRunning
        val isRoot = status.uid == 0
        val apiVersion = status.apiVersion
        val patchVersion = status.patchVersion
        if (ok) {
            icon.setImageResource(R.drawable.ic_server_ok_24dp)
        } else {
            icon.setImageResource(R.drawable.ic_server_error_24dp)
        }
        val user = if (isRoot) "root" else "adb"
        val title =
            if (ok) {
                getString(R.string.status_running)
            } else {
                getString(R.string.status_stopped)
            }
        val versionStr =
            getString(
                R.string.status_version,
                "$apiVersion.$patchVersion",
                user,
            )
        val updateStr =
            getString(
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
        this.title.text = title
        this.summary.text = summary
    }

    private fun HomeSimpleCardBinding.setup(
        title: CharSequence?,
        icon: Int,
        onClick: () -> Unit
    ) {
        this.title.text = title
        this.icon.setImageResource(icon)
        root.setOnClickListener { onClick() }
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

    fun start() {
        val cr = requireContext().contentResolver
        if (requireContext().hasWriteSecureSettings()) {
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        }

        val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
        if (adbEnabled == 0) {
            showWadbEnableUsbDebuggingDialog()
            return
        }

        val tcpPort = environmentUtils.getAdbTcpPort()
        val tcpMode = preferencesRepository.tcpMode.get()

        // If ADB is NOT listening to a TCP port and the device doesn't support TLS, inform the user
        if (tcpPort <= 0 && !environmentUtils.isTlsSupported()) {
            showWadbNotEnabledDialog()
            // Otherwise, just go straight to StartFragment and let it handle detection/searching
        } else if (tcpPort <= 0) {
            findNavController().navigate(R.id.navigate_to_start)
            // If ADB IS listening to a TCP port but the user wants to close it and use TLS instead, close the TCP port and start
        } else if (!tcpMode) {
            lifecycleScope.launch {
                adbStarter.stopTcp(tcpPort)
            }
            findNavController().navigate(R.id.navigate_to_start)
            // Otherwise ADB IS listening to a TCP port and the user wants to keep it open. Start Shizuku via TCP
        } else {
            findNavController().navigate(R.id.navigate_to_start)
        }
    }

    fun showWadbEnableUsbDebuggingDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.start_error_usb_debugging_disabled)
            .setPositiveButton(R.string.developer_options) { _, _ ->
                SystemSettingsPage.Developer.HighlightUsbDebugging.launch(requireContext())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showWadbNotEnabledDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.start_error_wireless_debugging_disabled)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onPairClicked(context: Context) {
        if (environmentUtils.isTelevision()) {
            showAccessibilityDialog(context)
        } else if (preferencesRepository.legacyPairing.get()) {
            (context as? FragmentActivity)?.supportFragmentManager?.let {
                // AdbPairDialogFragment().show(it) // TODO
            }
        } else {
            findNavController().navigate(R.id.navigate_to_pairing)
        }
    }

    private fun stop() {
        if (stateMachine.isRunning()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.stop_dialog_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    stateMachine.set(ShizukuStateMachine.State.STOPPING)
                    runCatching { Shizuku.exit() }
                }.setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }


    override fun onResume() {
        super.onResume()
        homeModel.reload()
        appsModel.load()
    }

    override fun onDestroyView() {
        stateMachine.removeListener(stateListener)
        super.onDestroyView()
    }
}
