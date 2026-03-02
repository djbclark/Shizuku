package moe.shizuku.manager.permission.ui.authorizedapps.components

import android.content.pm.PackageInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.databinding.AppListToggleAllBinding
import moe.shizuku.manager.permission.ui.authorizedapps.AppsAdapter
import rikka.recyclerview.BaseViewHolder

class ToggleAllViewHolder(private val binding: AppListToggleAllBinding) :
    BaseViewHolder<AppsAdapter.HeaderMarker>(binding.root), View.OnClickListener {

    companion object {
        @JvmField
        val CREATOR =
            Creator<AppsAdapter.HeaderMarker> { inflater: LayoutInflater, parent: ViewGroup? ->
                ToggleAllViewHolder(AppListToggleAllBinding.inflate(inflater, parent, false))
            }
    }

    private val switchWidget get() = binding.switchWidget

    init {
        itemView.filterTouchesWhenObscured = true
        itemView.setOnClickListener(this)
        itemView.applySystemBarsPadding(start = true, end = true)
    }

    override fun onClick(v: View) {
        setAllEnabled(!areAllEnabled())
        switchWidget.isChecked = areAllEnabled()
    }

    override fun onBind() {
        switchWidget.isChecked = areAllEnabled()
    }

    override fun onBind(payloads: List<Any>) {
        switchWidget.isChecked = areAllEnabled()
    }

    override fun onRecycle() {}

    private fun setAllEnabled(enabled: Boolean) {
        val items = adapter.getItems()
        for (item in items) {
            if (item is PackageInfo) {
                try {
                    if (enabled) {
                        AuthorizationManager.grant(item.packageName, item.applicationInfo!!.uid)
                    } else {
                        AuthorizationManager.revoke(item.packageName, item.applicationInfo!!.uid)
                    }
                } catch (_: Exception) {
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun areAllEnabled(): Boolean {
        val items = adapter.getItems()
        if (items.size <= 1) {
            return false
        }
        for (item in items) {
            if (item is PackageInfo) {
                try {
                    if (!AuthorizationManager.granted(
                            item.packageName,
                            item.applicationInfo!!.uid
                        )
                    ) return false
                } catch (_: Exception) {
                    return false
                }
            }
        }
        return true
    }
}
