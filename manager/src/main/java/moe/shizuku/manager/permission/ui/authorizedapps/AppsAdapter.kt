package moe.shizuku.manager.permission.ui.authorizedapps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.permission.models.AuthorizedAppsItem
import moe.shizuku.manager.permission.ui.authorizedapps.components.AppViewHolder
import moe.shizuku.manager.permission.ui.authorizedapps.components.ToggleAllViewHolder

class AppsAdapter(
    private val onAppClicked: (AuthorizedAppsItem.App) -> Unit,
    private val onToggleAllClicked: (Boolean) -> Unit
) : ListAdapter<AuthorizedAppsItem, RecyclerView.ViewHolder>(DiffCallback) {
    fun updateData(apps: List<AuthorizedAppsItem.App>, areAllGranted: Boolean) {
        val items = mutableListOf<AuthorizedAppsItem>().apply {
            if (apps.isNotEmpty()) {
                add(AuthorizedAppsItem.ToggleAll(areAllGranted))
                addAll(apps)
            }
        }
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int =
        getItem(position).viewType.ordinal


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val type = AuthorizedAppsItem.ViewType.entries.getOrNull(viewType)
            ?: throw IllegalStateException()

        val inflater = LayoutInflater.from(parent.context)
        return when (type) {
            AuthorizedAppsItem.ViewType.TOGGLE_ALL -> ToggleAllViewHolder(
                inflater.inflate(R.layout.app_list_toggle_all, parent, false),
                onToggleAllClicked
            )

            AuthorizedAppsItem.ViewType.APP -> AppViewHolder(
                inflater.inflate(R.layout.app_list_item, parent, false),
                onAppClicked
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ToggleAllViewHolder -> holder.bind(item as AuthorizedAppsItem.ToggleAll)
            is AppViewHolder -> holder.bind(getItem(position) as AuthorizedAppsItem.App)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AuthorizedAppsItem>() {
            override fun areItemsTheSame(
                oldItem: AuthorizedAppsItem,
                newItem: AuthorizedAppsItem
            ): Boolean {
                return when (oldItem) {
                    is AuthorizedAppsItem.App if newItem is AuthorizedAppsItem.App -> {
                        oldItem.appInfo.packageName == newItem.appInfo.packageName
                    }

                    is AuthorizedAppsItem.ToggleAll if newItem is AuthorizedAppsItem.ToggleAll -> {
                        true
                    }

                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: AuthorizedAppsItem,
                newItem: AuthorizedAppsItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
