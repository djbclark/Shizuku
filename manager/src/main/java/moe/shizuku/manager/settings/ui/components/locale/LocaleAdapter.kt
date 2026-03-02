package moe.shizuku.manager.settings.ui.components.locale

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.ui.LocaleHelper

class LocaleAdapter(
    private val items: List<LocaleHelper.LocaleEntry>,
    private val currentTag: String,
    private val onItemClick: (LocaleHelper.LocaleEntry) -> Unit
) : RecyclerView.Adapter<LocaleEntryViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = LocaleEntryViewHolder.from(parent)

    override fun onBindViewHolder(holder: LocaleEntryViewHolder, position: Int) {
        val item = items[position]
        holder.bind(
            item,
            isSelected = item.tag == currentTag,
            onItemClick
        )
    }

    override fun getItemCount() = items.size
}
