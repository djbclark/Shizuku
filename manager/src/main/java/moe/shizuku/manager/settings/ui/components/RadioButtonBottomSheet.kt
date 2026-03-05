package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import moe.shizuku.manager.databinding.RadioButtonBottomSheetBinding
import moe.shizuku.manager.databinding.RadioButtonListItemBinding

class RadioButtonBottomSheet<T>(
    private val context: Context,
    @get:StringRes private val titleRes: Int,
    private val entries: List<T>,
    private val getLabel: (T) -> String,
    @get:StringRes private val getDescription: (T) -> Int? = { null },
    private val isEnabled: (T) -> Boolean = { true },
    private val onConfirm: (T) -> Unit
) {
    fun show(currentValue: T) {
        val layoutInflater = LayoutInflater.from(context)
        val binding = RadioButtonBottomSheetBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(context)

        entries.forEach { entry ->
            val itemBinding = RadioButtonListItemBinding.inflate(layoutInflater, binding.container, false)
            val enabled = isEnabled(entry)

            itemBinding.apply {
                title.text = getLabel(entry)

                val descriptionText = getDescription(entry)
                description.text = descriptionText?.let {
                    context.getString(it)
                }
                description.isVisible = descriptionText != null

                radioButton.isChecked = entry == currentValue

                root.isEnabled = enabled
                title.isEnabled = enabled
                description.isEnabled = enabled
                radioButton.isEnabled = enabled

                root.setOnClickListener {
                    if (enabled) {
                        onConfirm(entry)
                        dialog.dismiss()
                    }
                }
            }
            binding.container.addView(itemBinding.root)
        }

        binding.apply {
            title.setText(titleRes)
            buttonClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.apply {
            setContentView(binding.root)
            show()
        }
    }
}
