package moe.shizuku.manager.permission.ui.authorizedapps.components

import android.content.pm.PackageInfo
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.databinding.AppListToggleAllBinding

class ToggleAllViewHolder(
    private val binding: AppListToggleAllBinding,
    private val permissionManager: PermissionManager,
    private val getItems: () -> List<Any>,
    private val onAuthorizationsChanged: () -> Unit
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    private val switchWidget get() = binding.switchWidget

    init {
        itemView.filterTouchesWhenObscured = true
        itemView.setOnClickListener(this)
        itemView.applySystemBarsPadding(start = true, end = true)
    }

    override fun onClick(v: View) {
        setAllEnabled(!areAllEnabled())
    }

    fun bind() {
        switchWidget.isChecked = areAllEnabled()
    }

    private fun setAllEnabled(enabled: Boolean) {
        val items = getItems()
        for (item in items) {
            if (item is PackageInfo) {
                try {
                    if (enabled) {
                        permissionManager.grant(item.applicationInfo!!.uid)
                    } else {
                        permissionManager.revoke(item.applicationInfo!!.uid)
                    }
                } catch (_: Exception) {
                }
            }
        }
        onAuthorizationsChanged()
    }

    private fun areAllEnabled(): Boolean {
        val items = getItems()
        val apps = items.filterIsInstance<PackageInfo>()
        if (apps.isEmpty()) {
            return false
        }
        for (item in apps) {
            try {
                if (!permissionManager.granted(item.applicationInfo!!.uid)) {
                    return false
                }
            } catch (_: Exception) {
                return false
            }
        }
        return true
    }
}
