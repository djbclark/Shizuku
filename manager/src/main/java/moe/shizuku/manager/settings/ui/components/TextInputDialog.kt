package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.TextInputDialogBinding

class TextInputDialog(
    private val context: Context,
    @param:StringRes private val title: Int,
    private val placeholder: String? = null,
    private val inputType: Int = InputType.TYPE_CLASS_TEXT,
    private val maxLength: Int? = null,
    private val inputValidation: ((String?) -> Int?)? = null,
    @param:StringRes private val positiveLabel: Int = R.string.save,
    private val onConfirm: (String) -> Unit
) {
    fun show(currentValue: Any) {
        val binding = TextInputDialogBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveLabel) { _, _ ->
                val input = binding.editText.text.toString()
                onConfirm(input)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        binding.inputLayout.placeholderText = placeholder

        binding.editText.apply {
            this.inputType = this@TextInputDialog.inputType
            this.filters = maxLength?.let {
                arrayOf(InputFilter.LengthFilter(it))
            } ?: emptyArray()

            setText(currentValue.toString())
            setSelection(text?.length ?: 0)

            addTextChangedListener { text ->
                val input = text.toString()
                val errorRes = inputValidation?.invoke(input)

                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton?.isEnabled = errorRes == null

                binding.inputLayout.error = errorRes?.let {
                    context.getString(it)
                }
            }
        }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        binding.editText.requestFocus()
    }
}