package moe.shizuku.manager.settings.ui.components.locale

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.ui.LocaleHelper
import moe.shizuku.manager.databinding.LocaleListItemBinding

class LocaleEntryViewHolder(val binding: LocaleListItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: LocaleHelper.LocaleEntry,
        isSelected: Boolean,
        onItemClick: (LocaleHelper.LocaleEntry) -> Unit
    ) {
        binding.apply {
            title.text = item.nameOwnLocale
            subtitle.text = item.nameCurrentLocale
            subtitle.isVisible = item.nameCurrentLocale.isNotBlank()

            root.isActivated = isSelected
            root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        fun from(parent: ViewGroup): LocaleEntryViewHolder {
            val binding = LocaleListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return LocaleEntryViewHolder(binding)
        }
    }
}