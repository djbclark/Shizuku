package moe.shizuku.manager.settings.ui

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionBottomSheet
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionViewModel
import moe.shizuku.manager.core.ui.helpers.LocaleHelper
import moe.shizuku.manager.settings.models.SettingsEvent
import moe.shizuku.manager.settings.models.SettingsUiState
import moe.shizuku.manager.settings.ui.components.TextInputDialog
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import moe.shizuku.manager.core.data.preferences.Preference as ShizukuPreference

class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModel()
    private val listSelectionViewModel: ListSelectionViewModel by viewModel()
    
    private val preferencesRepository: PreferencesRepository by inject()
    private val localeHelper: LocaleHelper by inject()
    private val powerManagerHelper: PowerManagerHelper by inject()

    // -------------------
    // PREFERENCES
    // -------------------

    private fun <T : Preference> find(pref: ShizukuPreference<*>): T = findPreference(pref.key)!!

    private val startModePreference: Preference by lazy { find(preferencesRepository.startMode) }
    private val startOnBootPreference: TwoStatePreference by lazy { find(preferencesRepository.startOnBoot) }
    private val watchdogPreference: TwoStatePreference by lazy { find(preferencesRepository.watchdog) }
    private val tcpModePreference: TwoStatePreference by lazy { find(preferencesRepository.tcpMode) }
    private val tcpPortPreference: Preference by lazy { find(preferencesRepository.tcpPort) }
    private val languagePreference: Preference by lazy { find(preferencesRepository.language) }
    private val themePreference: Preference by lazy { find(preferencesRepository.theme) }
    private val amoledBlackPreference: TwoStatePreference by lazy { find(preferencesRepository.amoledBlack) }
    private val dynamicColorPreference: TwoStatePreference by lazy { find(preferencesRepository.dynamicColor) }
    private val updateChannelPreference: Preference by lazy { find(preferencesRepository.updateChannel) }
    private val legacyPairingPreference: TwoStatePreference by lazy { find(preferencesRepository.legacyPairing) }
    private val wirelessDebuggingCategory: PreferenceCategory by lazy { findPreference("category_wireless_debugging")!! }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        initSharedPrefs()
        setupListeners()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCollectors()
    }

    // -------------------
    // LISTENERS
    // -------------------

    private fun setupListeners() {
        startModePreference.setOnPreferenceClickListener {
            showStartModeSelector()
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
            val currentPort = viewModel.uiState.value.tcpPortValue
            tcpPortInput.show(currentValue = currentPort)
            true
        }

        languagePreference.setOnPreferenceClickListener {
            showLanguageSelector()
            true
        }

        themePreference.setOnPreferenceClickListener {
            showThemeSelector()
            true
        }

        updateChannelPreference.setOnPreferenceClickListener {
            showUpdateChannelSelector()
            true
        }
    }

    private fun setupCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
                launch {
                    listSelectionViewModel.results.collect {
                        viewModel.handleSelectionResult(it)
                    }
                }
            }
        }
    }

    // -------------------
    // HANDLERS
    // -------------------

    private fun handleState(state: SettingsUiState) {
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
    // INPUTS
    // -------------------

    private fun showStartModeSelector() =
        ListSelectionBottomSheet.show(
            childFragmentManager,
            title = R.string.start_mode,
            footer = R.string.start_mode_footer,
            items = StartMode.entries,
            selectedItem = viewModel.uiState.value.startModeValue
        )

    private val tcpPortInput by lazy {
        TextInputDialog(
            context = requireContext(),
            title = R.string.settings_tcp_port,
            placeholder = preferencesRepository.tcpPort.default.toString(),
            inputType = InputType.TYPE_CLASS_NUMBER,
            maxLength = 5,
            inputValidation = { viewModel.validatePort(it) },
            onConfirm = { viewModel.onTcpPortChanged(it) }
        )
    }

    private fun showLanguageSelector() =
        ListSelectionBottomSheet.show(
            childFragmentManager,
            title = R.string.settings_language,
            items = localeHelper.getLocaleEntries(),
            selectedItem = viewModel.uiState.value.languageValue
        )

    private fun showThemeSelector() =
        ListSelectionBottomSheet.show(
            childFragmentManager,
            title = R.string.settings_theme,
            items = Theme.entries,
            selectedItem = viewModel.uiState.value.themeValue
        )

    private fun showUpdateChannelSelector() =
        ListSelectionBottomSheet.show(
            childFragmentManager,
            title = R.string.settings_update_channel,
            items = UpdateChannel.entries,
            selectedItem = viewModel.uiState.value.updateChannelValue
        )

    // -------------------
    // HELPER FUNCTIONS
    // -------------------

    private fun initSharedPrefs() {
        preferenceManager.sharedPreferencesName = PreferencesRepository.PREFS_NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.settings, null)

        // Needed to prevent flickering by overriding default visibility in settings.xml
        handleState(viewModel.uiState.value)
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
                val intent = powerManagerHelper.getBatteryOptimizationIntent()
                val batteryOptimizationLauncher =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        viewModel.onBatteryOptimizationResult()
                    }
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
