package moe.shizuku.manager.permission.ui.authorizedapps.components

import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.databinding.AppListToggleAllBinding
import moe.shizuku.manager.permission.models.AuthorizedAppsItem

class ToggleAllViewHolder(
    itemView: android.view.View,
    private val onToggleAllClicked: (Boolean) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val binding by viewBinding(AppListToggleAllBinding::bind)

    init {
        with(itemView) {
            filterTouchesWhenObscured = true
            setOnClickListener {
                onToggleAllClicked(!binding.switchWidget.isChecked)
            }
            applySystemBarsPadding(start = true, end = true)
        }
    }

    fun bind(item: AuthorizedAppsItem.ToggleAll) {
        binding.switchWidget.isChecked = item.areAllGranted
    }
}
