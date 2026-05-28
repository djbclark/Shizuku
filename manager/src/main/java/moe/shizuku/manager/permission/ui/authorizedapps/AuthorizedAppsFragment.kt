package moe.shizuku.manager.permission.ui.authorizedapps

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.collectAsEvents
import moe.shizuku.manager.core.extensions.snackbar
import moe.shizuku.manager.core.platform.device.RomInfo
import moe.shizuku.manager.core.platform.settings.SettingsIntentFactory
import moe.shizuku.manager.core.ui.components.dialog
import moe.shizuku.manager.core.ui.components.handleDialogResults
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.AuthorizedAppsFragmentBinding
import moe.shizuku.manager.permission.models.AuthorizedAppsEvent
import moe.shizuku.manager.permission.models.AuthorizedAppsUiState
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthorizedAppsFragment : Fragment(R.layout.authorized_apps_fragment) {

    private val viewModel: AuthorizedAppsViewModel by viewModel()
    private val settingsIntentFactory: SettingsIntentFactory by inject()

    private val adapter by lazy {
        AppsAdapter(
            onAppClicked = { viewModel.toggleApp(it) },
            onToggleAllClicked = { viewModel.toggleAll(it) }
        )
    }

    private val binding by viewBinding(AuthorizedAppsFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.adapter = adapter
        binding.list.applySystemBarsPadding(bottom = true)

        setupCollectors()
    }

    private fun setupCollectors() {
        handleDialogResults { key: AuthorizedAppsEvent.Dialog, success ->
            if (!success) return@handleDialogResults

            when (key) {
                AuthorizedAppsEvent.Dialog.ADB_RESTRICTED -> {
                    startActivity(settingsIntentFactory.developerOptions())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        handleUiState(state)
                    }
                }
                launch {
                    viewModel.events.collectAsEvents { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleUiState(state: AuthorizedAppsUiState) = with(binding) {
        when (state) {
            is AuthorizedAppsUiState.Loading -> {
                list.isVisible = false
                empty.root.isVisible = false

                // TODO implement loading screen
            }

            is AuthorizedAppsUiState.Success -> {
                list.isVisible = !state.isAppListEmpty
                empty.root.isVisible = state.isAppListEmpty

                if (!state.isAppListEmpty) {
                    adapter.updateData(state.apps, state.areAllAppsGranted)
                }
            }

            is AuthorizedAppsUiState.Error -> {
                // TODO
            }
        }
    }

    private fun handleEvent(event: AuthorizedAppsEvent) {
        when (event) {
            AuthorizedAppsEvent.NotifyAdbRestricted -> {
                showAdbRestrictedDialog()
            }

            is AuthorizedAppsEvent.ShowError -> {
                snackbar(event.error)
            }
        }
    }

    private fun showAdbRestrictedDialog() {
        val restriction = when {
            RomInfo.isMiui || RomInfo.isHyperOs -> getString(R.string.status_adb_restricted_miui)
            RomInfo.isColorOs -> getString(R.string.status_adb_restricted_color_os)
            RomInfo.isFlyme -> getString(R.string.status_adb_restricted_flyme)
            else -> null
        }

        val message = if (restriction != null) {
            getString(R.string.status_adb_restricted_message, restriction)
        } else {
            getString(R.string.status_adb_restricted)
        }

        dialog()
            .setTitle(R.string.status_adb_restricted)
            .setMessage(message)
            .setPositiveButton(R.string.developer_options)
            .addCancelButton()
            .show(childFragmentManager, AuthorizedAppsEvent.Dialog.ADB_RESTRICTED)
    }
}
