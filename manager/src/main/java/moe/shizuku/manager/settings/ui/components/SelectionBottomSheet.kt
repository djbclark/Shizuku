package moe.shizuku.manager.settings.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.ui.components.StyledBottomSheet
import moe.shizuku.manager.databinding.RadioButtonListItemBinding

class SelectionBottomSheet : StyledBottomSheet() {

    companion object {
        fun <T : Any> show(
            fragmentManager: FragmentManager,
            viewModel: SelectionViewModel,
            @StringRes title: Int,
            @StringRes footer: Int? = null,
            items: List<SelectionItem<T>>,
            selectedValue: T?
        ) {
            viewModel.items = items
            viewModel.selectedValue = selectedValue

            SelectionBottomSheet().apply {
                this.titleRes = title
                this.footerRes = footer
            }.show(fragmentManager, TAG)
        }
    }

    private val viewModel: SelectionViewModel by viewModels({ requireParentFragment() })

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?): View =
        RecyclerView(requireContext()).apply {
            id = android.R.id.list
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(context)
            adapter = RadioButtonAdapter()
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

    private inner class RadioButtonAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                RadioButtonListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(viewModel.items[position])
        }

        override fun getItemCount(): Int = viewModel.items.size
    }

    private inner class ViewHolder(private val binding: RadioButtonListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectionItem<Any>) {
            binding.apply {
                title.text = item.label
                description.text = item.description
                description.isVisible = item.description != null

                radioButton.isChecked = item.value == viewModel.selectedValue
                root.apply {
                    isEnabled = item.isEnabled
                    alpha = if (item.isEnabled) 1f else 0.38f
                    setOnClickListener {
                        if (item.isEnabled) {
                            viewModel.select(item.value)
                            dismiss()
                        }
                    }
                }
            }
        }
    }
}
