package moe.shizuku.manager.settings.ui

import android.content.Context
import android.os.Bundle
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
import moe.shizuku.manager.core.data.KeyValueDataSource
import moe.shizuku.manager.core.data.KeyValueEntry
import moe.shizuku.manager.core.data.preferences.PreferenceKeys
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.ui.components.snackbar
import moe.shizuku.manager.settings.models.SettingsEvent
import moe.shizuku.manager.settings.models.SettingsUiState
import moe.shizuku.manager.settings.ui.components.RadioButtonDialog
import moe.shizuku.manager.settings.ui.components.TcpPortDialog
import moe.shizuku.manager.settings.ui.components.locale.LocaleDialog

class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    private fun <T : Preference> find(entry: KeyValueEntry<*>): T =
        findPreference(entry.key)!!

    private val startOnBootPreference: TwoStatePreference
            by lazy { find(PreferenceKeys.START_ON_BOOT) }
    private val watchdogPreference: TwoStatePreference
            by lazy { find(PreferenceKeys.WATCHDOG) }
    private val tcpModePreference: TwoStatePreference
            by lazy { find(PreferenceKeys.TCP_MODE) }
    private val tcpPortPreference: Preference
            by lazy { find(PreferenceKeys.TCP_PORT) }
    private val autoDisableUsbDebuggingPreference: TwoStatePreference
            by lazy { find(PreferenceKeys.AUTO_DISABLE_USB_DEBUGGING) }
    private val languagePreference: Preference
            by lazy { find(PreferenceKeys.LANGUAGE) }
    private val themePreference: Preference
            by lazy { find(PreferenceKeys.THEME) }
    private val amoledBlackPreference: TwoStatePreference
            by lazy { find(PreferenceKeys.AMOLED_BLACK) }
    private val dynamicColorPreference: TwoStatePreference
            by lazy { find(PreferenceKeys.DYNAMIC_COLOR) }
    private val updateChannelPreference: Preference
            by lazy { find(PreferenceKeys.UPDATE_CHANNEL) }
    private val legacyPairingPreference: TwoStatePreference
            by lazy { find(PreferenceKeys.LEGACY_PAIRING) }
    private val advancedCategory: PreferenceCategory
            by lazy { findPreference("category_advanced")!! }

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onBatteryOptimizationResult()
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        initSharedPrefs()

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
            showTcpPortDialog()
            true
        }

        languagePreference.setOnPreferenceClickListener {
            LocaleDialog.show(requireContext())
            true
        }

        themePreference.setOnPreferenceClickListener {
            showThemeDialog()
            true
        }

        amoledBlackPreference.setOnPreferenceChangeListener { _, _ ->
            viewModel.onAmoledBlackChanged()
            true
        }

        dynamicColorPreference.setOnPreferenceChangeListener { _, _ ->
            viewModel.onDynamicColorChanged()
            true
        }

        updateChannelPreference.setOnPreferenceClickListener {
            showUpdateChannelDialog()
            true
        }

        updateUi(viewModel.uiState.value)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

        tcpModePreference.apply {
            isVisible = state.isTcpModeVisible
            isChecked = state.tcpModeValue
        }

        tcpPortPreference.apply {
            isVisible = state.isTcpPortVisible
            summary = state.tcpPortValue.toString()
        }

        autoDisableUsbDebuggingPreference.isVisible = state.isAutoDisableUsbDebuggingVisible

        languagePreference.summary =
            state.languageValue ?: getString(R.string.follow_system)

        themePreference.summary = getString(state.themeValue.labelRes)
        amoledBlackPreference.isVisible = state.isAmoledBlackVisible
        dynamicColorPreference.isVisible = state.isDynamicColorVisible

        updateChannelPreference.summary = getString(state.updateChannelValue.labelRes)

        legacyPairingPreference.isVisible = state.isLegacyPairingVisible
        advancedCategory.isVisible = state.isLegacyPairingVisible
    }

    private fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.RequestBatteryOptimization -> {
                showBatteryOptimizationSnackbar()
            }

            is SettingsEvent.PromptRestart -> {
                promptRestart(event.setting, event.newValue)
            }

            is SettingsEvent.PromptStopTcp -> {
                promptStopTcp()
            }

            is SettingsEvent.ShowStartOnBootBugDialog -> {
                showStartOnBootBugDialog()
            }

            is SettingsEvent.RecreateActivity -> {
                activity?.recreate()
            }
        }
    }

    // -------------------
    // HELPER FUNCTIONS
    // -------------------

    private fun initSharedPrefs() {
        preferenceManager.sharedPreferencesName = KeyValueDataSource.PREFS_NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.settings, null)
    }

    private fun showTcpPortDialog() {
        val currentPort = tcpPortPreference.summary.toString().toInt()
        TcpPortDialog.show(requireContext(), currentPort) {
            viewModel.onTcpPortChanged(it)
        }
    }

    private fun showThemeDialog() =
        RadioButtonDialog.show(
            context = requireContext(),
            titleRes = R.string.settings_theme,
            entries = Theme.entries,
            currentValue = viewModel.uiState.value.themeValue,
            getLabel = { getString(it.labelRes) },
            onConfirm = { viewModel.onThemeChanged(it) }
        )

    private fun showUpdateChannelDialog() =
        RadioButtonDialog.show(
            context = requireContext(),
            titleRes = R.string.settings_update_channel,
            entries = UpdateChannel.entries,
            currentValue = viewModel.uiState.value.updateChannelValue,
            getLabel = { getString(it.labelRes) },
            onConfirm = { viewModel.onUpdateChannelChanged(it) }
        )

    private fun showStartOnBootBugDialog() =
        MaterialAlertDialogBuilder(requireContext()).setTitle(android.R.string.dialog_alert_title)
            .setMessage(R.string.settings_start_on_boot_bug)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.applyStartOnBootChange(true)
            }.setNegativeButton(android.R.string.cancel, null).show()

    private fun showBatteryOptimizationSnackbar() = snackbar(
        msg = R.string.settings_battery_optimization, actionText = R.string.fix, action = {
            val intent = PowerManagerHelper.getBatteryOptimizationIntent()
            batteryOptimizationLauncher.launch(intent)
        })

    private fun promptStopTcp() =
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.tcp_close_port)
            .setMessage(requireContext().getString(R.string.tcp_close_port_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onStopTcp(requireContext())
            }.setNegativeButton(android.R.string.cancel, null).show()

    private fun promptRestart(
        pref: KeyValueEntry<*>,
        newValue: Any
    ) {
        val msg = buildString {
            append(getString(R.string.tcp_restart_required_message))
            if (pref == PreferenceKeys.TCP_MODE) {
                append("\n\n")
                append(getString(R.string.tcp_restart_required_message_wifi_required))
            }
        }

        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.tcp_restart_required)
            .setMessage(msg).setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onRestart(pref, newValue, requireContext())
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView =
        super.onCreateRecyclerView(inflater, parent, savedInstanceState).also {
            it.applySystemBarsPadding(bottom = true, start = true, end = true)
        }
}
