package moe.shizuku.manager.settings.ui.components

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object RadioButtonDialog {
    fun <T> show(
        context: Context,
        @StringRes titleRes: Int,
        entries: List<T>,
        currentValue: T,
        getLabel: (T) -> String,
        onConfirm: (T) -> Unit
    ) {
        val options = entries.map { getLabel(it) }.toTypedArray()
        var selectedIndex = entries.indexOf(currentValue)

        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onConfirm(entries[selectedIndex])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}