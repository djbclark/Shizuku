package moe.shizuku.manager.intents.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.databinding.IntentsFragmentBinding
import moe.shizuku.manager.intents.models.IntentsUiState
import org.koin.androidx.viewmodel.ext.android.viewModel

class IntentsFragment : Fragment(R.layout.intents_fragment) {
    private val viewModel: IntentsViewModel by viewModel()
    private val binding by viewBinding(IntentsFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fieldPackage.text = requireContext().packageName
        setupListeners()

        viewModel.uiState
            .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .onEach { handleState(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupListeners() = with(binding) {
        buttonRegenerateExtra.setOnClickListener {
            promptRegenerateToken()
        }

        buttonGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val action = when (buttonId) {
                R.id.buttonStart -> IntentsUiState.IntentAction.START
                R.id.buttonStop -> IntentsUiState.IntentAction.STOP
                else -> return@addOnButtonCheckedListener
            }
            viewModel.onIntentActionChanged(action)
        }
    }

    private fun handleState(
        state: IntentsUiState
    ) = with(binding) {
        fieldExtra.text = state.authToken
        fieldAction.text = state.intentAction.string

        setEnabledState(state.enabled)
        setCheckedButton(state.intentAction)
    }

    private fun setEnabledState(enabled: Boolean) = with(binding) {
        val excludedIds = listOf(
            R.id.description
        )
        container.children.forEach { view ->
            if (!excludedIds.contains(view.id)) {
                view.isEnabled = enabled
            }
        }
    }

    private fun setCheckedButton(action: IntentsUiState.IntentAction) = with(binding) {
        val buttonToCheck = when (action) {
            IntentsUiState.IntentAction.START -> R.id.buttonStart
            IntentsUiState.IntentAction.STOP -> R.id.buttonStop
        }

        if (buttonGroup.checkedButtonId == buttonToCheck) return@with
        buttonGroup.check(buttonToCheck)
    }

    private fun promptRegenerateToken() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.intents_token_regenerate)
            .setMessage(R.string.intents_token_regenerate_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.onRegenerateToken()
            }
            .show()
    }
}
