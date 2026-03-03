package moe.shizuku.manager.settings.ui.components

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R

class RadioButtonDialog<T>(
    private val context: Context,
    @get:StringRes private val titleRes: Int,
    private val entries: List<T>,
    private val getLabel: (T) -> String,
    @get:StringRes private val positiveLabel: Int = R.string.save,
    private val onConfirm: (T) -> Unit
) {
    fun show(currentValue: T) {
        val options = entries.map { getLabel(it) }.toTypedArray()
        var selectedIndex = entries.indexOf(currentValue)

        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(positiveLabel) { _, _ ->
                onConfirm(entries[selectedIndex])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}