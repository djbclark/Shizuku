package moe.shizuku.manager.home

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.autostart.AutoStartManager
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.collectAsEvents
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.extensions.openUrl
import moe.shizuku.manager.core.extensions.snackbar
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.platform.services.BatteryOptimizationHelper
import moe.shizuku.manager.core.platform.settings.DeveloperOptionsSetting
import moe.shizuku.manager.core.platform.settings.SettingsIntentFactory
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.preferences.models.StartMode
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionViewModel
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.HomeFragmentBinding
import moe.shizuku.manager.databinding.HomeSimpleCardBinding
import moe.shizuku.manager.databinding.HomeStatusCardBinding
import moe.shizuku.manager.home.models.HomeEvent
import moe.shizuku.manager.home.models.PrivilegedServiceUiState
import moe.shizuku.manager.pairing.ui.showAccessibilityDialog
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.PrivilegedServiceStateMachine
import moe.shizuku.manager.start.models.PreStartCheckError
import moe.shizuku.manager.updater.UpdateHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : Fragment(R.layout.home_fragment) {
    private val homeModel: HomeViewModel by viewModel()
    private val listSelectionModel: ListSelectionViewModel by viewModel()
    private val preferencesRepository: PreferencesRepository by inject()
    private val batteryOptimizationHelper: BatteryOptimizationHelper by inject()
    private val updateHelper: UpdateHelper by inject()
    private val settingsIntentFactory: SettingsIntentFactory by inject()
    private val privilegedServiceManager: PrivilegedServiceManager by inject()
    private val privilegedServiceStateMachine: PrivilegedServiceStateMachine by inject()
    private val adbSettingsManager: AdbSettingsManager by inject()
    private val adbPortHelper: AdbPortHelper by inject()
    private val autoStartManager: AutoStartManager by inject()

    private val binding by viewBinding(HomeFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        setupCards()

        requireActivity().addMenuProvider(
            HomeMenuProvider(this),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeModel.uiState.collect { state ->
                        binding.statusCard.update(state.serviceState)
                        binding.authorizedAppsCard.summary.apply {
                            isVisible = true
                            text = resources.getQuantityString(
                                R.plurals.authorized_apps_count,
                                state.authorizedAppsCount,
                                state.authorizedAppsCount
                            )
                        }
                    }
                }
                launch {
                    homeModel.events.collectAsEvents { event ->
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

//        lifecycleScope.launch {
//            if (updateHelper.isCheckForUpdatesEnabled() && updateHelper.isNewUpdateAvailable()) {
//                snackbar(
//                    msg = getString(R.string.update_available),
//                    duration = Snackbar.LENGTH_INDEFINITE
//                ).setAction(R.string.update) {
//                    lifecycleScope.launch {
//                        updateHelper.update()
//                    }
//                }.show()
//                updateHelper.updateLastPromptedVersion()
//            }
//        }
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
                    startActivity(batteryOptimizationHelper.intent)
                }.show()
            }
        }
    }

    private fun setupCards() = with(binding) {
        statusCard.apply {
            buttonStart.apply {
                isVisible = adbPortHelper.tcpPort.isOk ||
                        adbSettingsManager.hasWirelessDebugging ||
                        preferencesRepository.startMode.get() == StartMode.ROOT
                setOnClickListener {
                    lifecycleScope.launch {
                        start()
//                        autoStartManager.start()
                    }
                }
            }

            buttonStop.setOnClickListener {
                stop()
            }

            if (adbSettingsManager.hasWirelessDebugging) {
                buttonPair.setOnClickListener {
                    onPairClicked()
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

    private fun HomeStatusCardBinding.update(status: PrivilegedServiceUiState) {
        when (status) {
            is PrivilegedServiceUiState.Running -> {
                val user = when (status.mode) {
                    PrivilegedServiceUiState.Running.Mode.ROOT -> "root"
                    PrivilegedServiceUiState.Running.Mode.ADB -> "adb"
                }

                val updateStr = getString(R.string.status_version_update)

                title.text = getString(R.string.status_running)
                icon.setImageResource(R.drawable.ic_server_ok_24dp)
                summary.text = buildString {
                    append(status.version)
                    append(", ")
                    append(user)
                    if (!status.isLatestVersion) append(". $updateStr")
                }
            }

            is PrivilegedServiceUiState.Stopped -> {
                title.text = getString(R.string.status_stopped)
                icon.setImageResource(R.drawable.ic_server_error_24dp)
                summary.text = null
            }
        }
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

    suspend fun start() {
        privilegedServiceManager.checkForegroundStart()
            .onOk { findNavController().navigate(R.id.navigate_to_start) }
            .onErr {
                val msgRes = when (it) {
                    PreStartCheckError.NotRooted -> R.string.start_error_root
                    PreStartCheckError.TlsNotSupported -> R.string.start_error_tls_not_supported
                    PreStartCheckError.UsbDebuggingDisabled -> R.string.start_error_usb_debugging_disabled
                    PreStartCheckError.WirelessDebuggingDisabled -> R.string.start_error_wireless_debugging_disabled
                    PreStartCheckError.WifiRequired -> R.string.start_error_wifi_required
                    PreStartCheckError.AuthorizationRequired -> R.string.start_error_authorization_required
                    PreStartCheckError.WriteSecureSettingsNotGranted -> 0
                }
                snackbar(msgRes).run {
                    if (it == PreStartCheckError.WirelessDebuggingDisabled) {
                        setAction(R.string.enable) {
                            startActivity(settingsIntentFactory.wirelessDebugging())
                        }
                    } else if (it == PreStartCheckError.UsbDebuggingDisabled) {
                        setAction(R.string.enable) {
                            val devOptions = settingsIntentFactory.developerOptions(
                                highlight = DeveloperOptionsSetting.UsbDebugging
                            )
                            startActivity(devOptions)
                        }
                    }
                    show()
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onPairClicked() {
        if (requireContext().isTelevision) {
            showAccessibilityDialog(requireContext())
        } else if (preferencesRepository.legacyPairing.get()) {
            (requireContext() as? FragmentActivity)?.supportFragmentManager?.let {
                // AdbPairDialogFragment().show(it) // TODO
            }
        } else {
            findNavController().navigate(R.id.navigate_to_pairing)
        }
    }

    private fun stop() {
        if (privilegedServiceStateMachine.isRunning) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.stop_dialog_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    privilegedServiceManager.stopService()
                }.setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }


    override fun onResume() {
        super.onResume()
        homeModel.checkPermissionOwner()
    }

}
