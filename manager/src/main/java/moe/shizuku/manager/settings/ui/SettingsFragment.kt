package moe.shizuku.manager.settings.ui

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.showSnackbar
import moe.shizuku.manager.core.extensions.snackbar
import moe.shizuku.manager.core.ui.LocaleHelper
import moe.shizuku.manager.settings.models.SettingsEvent
import moe.shizuku.manager.settings.models.SettingsUiState
import moe.shizuku.manager.settings.ui.components.SelectionBottomSheet
import moe.shizuku.manager.settings.ui.components.SelectionBottomSheet.SelectionItem
import moe.shizuku.manager.settings.ui.components.TextInputDialog
import moe.shizuku.manager.core.data.preferences.Preference as ShizukuPreference

class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    private fun <T : Preference> find(pref: ShizukuPreference<*>): T = findPreference(pref.key)!!

    private val startModePreference: Preference by lazy { find(PreferencesRepository.startMode) }
    private val startOnBootPreference: TwoStatePreference by lazy { find(PreferencesRepository.startOnBoot) }
    private val watchdogPreference: TwoStatePreference by lazy { find(PreferencesRepository.watchdog) }
    private val tcpModePreference: TwoStatePreference by lazy { find(PreferencesRepository.tcpMode) }
    private val tcpPortPreference: Preference by lazy { find(PreferencesRepository.tcpPort) }
    private val languagePreference: Preference by lazy { find(PreferencesRepository.language) }
    private val themePreference: Preference by lazy { find(PreferencesRepository.theme) }
    private val amoledBlackPreference: TwoStatePreference by lazy { find(PreferencesRepository.amoledBlack) }
    private val dynamicColorPreference: TwoStatePreference by lazy { find(PreferencesRepository.dynamicColor) }
    private val updateChannelPreference: Preference by lazy { find(PreferencesRepository.updateChannel) }
    private val legacyPairingPreference: TwoStatePreference by lazy { find(PreferencesRepository.legacyPairing) }
    private val wirelessDebuggingCategory: PreferenceCategory by lazy { findPreference("category_wireless_debugging")!! }

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onBatteryOptimizationResult()
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        initSharedPrefs()

        startModePreference.setOnPreferenceClickListener {
            val currentStartMode = viewModel.uiState.value.startModeValue
            startModeSelector.show(currentValue = currentStartMode)
            true
        }

        startOnBootPreference.setOnPreferenceChangeListener { _, newValue ->
            viewModel.onStartOnBootChanged(newValue as Boolean)
            false
        }

        watchdogPreference.setOnPreferenceChangeListener { _, newValue ->
            viewModel.onWatchdogChanged(newValue as Boolean)
            false
        }

        tcpModePreference.setOnPreferenceChangeListener { _, newValue ->
            viewModel.onTcpModeChanged(newValue as Boolean)
            false
        }

        tcpPortPreference.setOnPreferenceClickListener {
            val currentPort = tcpPortPreference.summary.toString().toInt()
            tcpPortInput.show(currentValue = currentPort)
            true
        }

        languagePreference.setOnPreferenceClickListener {
            val currentLanguage = viewModel.uiState.value.languageValue
            languageSelector.show(currentValue = currentLanguage)
            true
        }

        themePreference.setOnPreferenceClickListener {
            val currentTheme = viewModel.uiState.value.themeValue
            themeSelector.show(currentValue = currentTheme)
            true
        }

        updateChannelPreference.setOnPreferenceClickListener {
            val currentUpdateChannel = viewModel.uiState.value.updateChannelValue
            updateChannelSelector.show(currentValue = currentUpdateChannel)
            true
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUi(state)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUi(state: SettingsUiState) {
        startModePreference.summary = getString(state.startModeValue.labelRes)
        startOnBootPreference.apply {
            isEnabled = state.isStartOnBootToggleable
            isChecked = state.startOnBootValue
            summary = if (state.isStartOnBootToggleable) {
                null
            } else {
                getString(R.string.settings_start_on_boot_summary)
            }
        }
        watchdogPreference.isChecked = state.watchdogValue

        wirelessDebuggingCategory.isVisible = state.isWirelessDebuggingCategoryVisible
        tcpModePreference.apply {
            isVisible = state.isTcpModeVisible
            isChecked = state.tcpModeValue
        }
        tcpPortPreference.apply {
            isVisible = state.isTcpPortVisible
            summary = state.tcpPortValue.toString()
        }
        legacyPairingPreference.isVisible = state.isLegacyPairingVisible

        languagePreference.summary = state.languageValue.nameOwnLocale.takeUnless { it.isEmpty() }
            ?: getString(R.string.settings_system)
        themePreference.summary = getString(state.themeValue.labelRes)
        amoledBlackPreference.isVisible = state.isAmoledBlackVisible
        dynamicColorPreference.isVisible = state.isDynamicColorVisible

        updateChannelPreference.summary = getString(state.updateChannelValue.labelRes)
    }

    private fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.RequestBatteryOptimization -> {
                showBatteryOptimizationSnackbar()
            }

            is SettingsEvent.Snackbar -> {
                showSnackbar(event.msg)
            }

            is SettingsEvent.PromptStopTcp -> {
                promptStopTcp()
            }

            is SettingsEvent.ShowStartOnBootBugDialog -> {
                showStartOnBootBugDialog()
            }
        }
    }

    // -------------------
    // SELECTORS
    // -------------------

    private val startModeSelector by lazy {
        SelectionBottomSheet(
            context = requireContext(),
            titleRes = R.string.start_mode,
            footerRes = R.string.start_mode_footer,
            entries = StartMode.entries,
            itemMapper = {
                SelectionItem(
                    label = getString(it.labelRes),
                    description = viewModel.getStartModeDescription(it)?.let { description ->
                        getString(description)
                    },
                    isEnabled = viewModel.getStartModeSelectable(it)
                )
            },
            onConfirm = { viewModel.onStartModeChanged(it) })
    }

    private val tcpPortInput by lazy {
        TextInputDialog(
            context = requireContext(),
            titleRes = R.string.settings_tcp_port,
            placeholder = PreferencesRepository.tcpPort.default.toString(),
            inputType = InputType.TYPE_CLASS_NUMBER,
            maxLength = 5,
            inputValidation = { viewModel.validatePort(it) },
            onConfirm = { viewModel.onTcpPortChanged(it) })
    }

    private val languageSelector by lazy {
        SelectionBottomSheet(
            context = requireContext(),
            titleRes = R.string.settings_language,
            entries = LocaleHelper.getLocaleEntries(requireContext()),
            itemMapper = { locale ->
                SelectionItem(
                    label = locale.nameOwnLocale.takeUnless { it.isBlank() } ?: getString(
                        R.string.settings_system
                    ),
                    description = locale.nameCurrentLocale.takeUnless { it.isBlank() }
                )
            },
            onConfirm = { LocaleHelper.setLocale(it) })
    }

    private val themeSelector by lazy {
        SelectionBottomSheet(
            context = requireContext(),
            titleRes = R.string.settings_theme,
            entries = Theme.entries,
            itemMapper = {
                SelectionItem(
                    label = getString(it.labelRes)
                )
            },
            onConfirm = { viewModel.onThemeChanged(it) })
    }

    private val updateChannelSelector by lazy {
        SelectionBottomSheet(
            context = requireContext(),
            titleRes = R.string.settings_update_channel,
            entries = UpdateChannel.entries,
            itemMapper = {
                SelectionItem(
                    label = getString(it.labelRes)
                )
            },
            onConfirm = { viewModel.onUpdateChannelChanged(it) })
    }

    // -------------------
    // HELPER FUNCTIONS
    // -------------------

    private fun initSharedPrefs() {
        preferenceManager.sharedPreferencesName = PreferencesRepository.PREFS_NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.settings, null)
        
        // Needed to prevent flickering by overriding default visibility in settings.xml
        updateUi(viewModel.uiState.value)
    }

    private fun showStartOnBootBugDialog() =
        MaterialAlertDialogBuilder(requireContext()).setTitle(android.R.string.dialog_alert_title)
            .setMessage(R.string.settings_start_on_boot_bug)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.applyStartOnBootChange(true)
            }.setNegativeButton(android.R.string.cancel, null).show()

    private fun showBatteryOptimizationSnackbar() =
        snackbar(R.string.settings_battery_optimization)
            .setAction(R.string.fix) {
                val intent = PowerManagerHelper.getBatteryOptimizationIntent()
                batteryOptimizationLauncher.launch(intent)
            }

    private fun promptStopTcp() =
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.tcp_close_port)
            .setMessage(requireContext().getString(R.string.tcp_close_port_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onStopTcp(requireContext())
            }.setNegativeButton(android.R.string.cancel, null).show()

    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState).also {
        it.applySystemBarsPadding(bottom = true, start = true, end = true)
    }
}
