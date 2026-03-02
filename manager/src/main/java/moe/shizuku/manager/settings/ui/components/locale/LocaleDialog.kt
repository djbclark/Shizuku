package moe.shizuku.manager.settings.ui.components.locale

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.LocaleHelper

object LocaleDialog {

    fun show(context: Context) {
        val items = LocaleHelper.getLocaleEntries(context)
        val currentTag = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: ""

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_language)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        val recyclerView = RecyclerView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val topPadding = context.resources.getDimensionPixelSize(R.dimen.dialog_top_padding)
            setPadding(paddingLeft, topPadding, paddingRight, paddingBottom)

            layoutManager = LinearLayoutManager(context)
            adapter = LocaleAdapter(items, currentTag) { item ->
                dialog.dismiss()
                LocaleHelper.setLocale(item)
            }
        }

        dialog.setView(recyclerView)
        dialog.show()
    }
}
