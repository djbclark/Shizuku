package moe.shizuku.manager.permission.ui.authorizedapps.components

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.setAppIcon
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.AppListItemBinding
import moe.shizuku.manager.permission.models.AuthorizedAppsItem

class AppViewHolder(
    itemView: View,
    private val onAppClicked: (AuthorizedAppsItem.App) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val binding by viewBinding(AppListItemBinding::bind)

    private var item: AuthorizedAppsItem.App? = null

    init {
        with(itemView) {
            filterTouchesWhenObscured = true
            setOnClickListener {
                item?.let { onAppClicked(it) }
            }
            applySystemBarsPadding(start = true, end = true)
        }
    }

    fun bind(item: AuthorizedAppsItem.App) {
        this.item = item

        binding.icon.setAppIcon(item.appInfo)
        binding.title.text = item.displayName
        binding.summary.text = item.packageName
        binding.switchWidget.isChecked = item.isGranted

    }
}
