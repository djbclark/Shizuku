package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.preferences.PreferenceKeys
import moe.shizuku.manager.databinding.TcpPortInputDialogBinding

object TcpPortDialog {

    fun show(
        context: Context,
        currentPort: Int,
        onSave: (Int) -> Unit
    ) {
        val binding = TcpPortInputDialogBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_tcp_port)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val input = binding.editText.text.toString()
                val port = input.toIntOrNull() ?: PreferenceKeys.TCP_PORT.default
                onSave(port)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        binding.inputLayout.placeholderText = PreferenceKeys.TCP_PORT.default.toString()

        binding.editText.apply {
            setText(currentPort.toString())
            setSelection(text?.length ?: 0)
            addTextChangedListener { text ->
                val port = text.toString().toIntOrNull()
                val isValid = (port == null) || (port in 1..65535)
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton?.isEnabled = isValid

                binding.inputLayout.error =
                    if (isValid) null else context.getString(R.string.tcp_error_invalid_port)
            }
        }

        dialog.show()
        binding.editText.requestFocus()
    }
}