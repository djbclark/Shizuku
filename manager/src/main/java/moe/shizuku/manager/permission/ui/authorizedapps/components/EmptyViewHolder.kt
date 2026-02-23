package moe.shizuku.manager.permission.ui.authorizedapps.components

import android.view.LayoutInflater
import android.view.ViewGroup
import moe.shizuku.manager.databinding.AppListEmptyBinding
import rikka.recyclerview.BaseViewHolder

class EmptyViewHolder(
    private val binding: AppListEmptyBinding,
) : BaseViewHolder<Any>(binding.root) {
    companion object {
        @JvmField
        val CREATOR =
            Creator<Any> {
                    inflater: LayoutInflater,
                    parent: ViewGroup?,
                ->
                EmptyViewHolder(AppListEmptyBinding.inflate(inflater, parent, false))
            }
    }
}
