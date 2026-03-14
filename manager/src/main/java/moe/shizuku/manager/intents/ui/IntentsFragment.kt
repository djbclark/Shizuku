package moe.shizuku.manager.intents.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.IntentsFragmentBinding
import moe.shizuku.manager.intents.data.TokenRepository

class IntentsFragment : Fragment(R.layout.intents_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = IntentsFragmentBinding.bind(view)

        val authToken = TokenRepository.getAuthToken()

        binding.apply {
            fieldAction.text = getIntentAction(buttonGroup.checkedButtonId)
            fieldPackage.text = requireContext().packageName
            fieldExtra.text = authToken

            buttonRegenerateExtra.setOnClickListener {
                promptRegenerateToken {
                    val newToken = TokenRepository.regenerateAuthToken()
                    fieldExtra.text = newToken
                }
            }

            buttonGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
                if (isChecked) {
                    fieldAction.text = getIntentAction(buttonId)
                }
            }
        }
    }

    private fun getIntentAction(buttonId: Int): String =
        when (buttonId) {
            R.id.buttonStart -> "${BuildConfig.APPLICATION_ID}.START"
            R.id.buttonStop -> "${BuildConfig.APPLICATION_ID}.STOP"
            else -> ""
        }

    private fun promptRegenerateToken(onConfirm: () -> Unit = {}) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.intents_token_regenerate)
            .setMessage(R.string.intents_token_regenerate_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onConfirm()
            }
            .show()
    }
}
