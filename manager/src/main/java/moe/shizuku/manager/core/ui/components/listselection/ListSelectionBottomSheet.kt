package moe.shizuku.manager.core.ui.components.listselection

import android.content.Context
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

class ListSelectionBottomSheet : StyledBottomSheet() {

    companion object {
        fun <T : Any> show(
            fragmentManager: FragmentManager,
            @StringRes title: Int,
            @StringRes footer: Int? = null,
            items: List<ListSelectionItem<T>>,
            selectedItem: T? = null
        ) {
            ListSelectionBottomSheet().apply {
                this.titleRes = title
                this.footerRes = footer
                this.items = items
                this.selectedItem = selectedItem
            }.show(fragmentManager, TAG)
        }
    }

    private val viewModel: ListSelectionViewModel by viewModels({ requireParentFragment() })

    // Items and selectedItem will be null on config change
    // Only update the ViewModel on the initial show() call
    private var items: List<ListSelectionItem<Any>>? = null
    private var selectedItem: Any? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        items?.let { viewModel.items = it }
        selectedItem?.let { viewModel.selectedItem = it }
    }

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

        fun bind(item: ListSelectionItem<Any>) {
            binding.apply {
                title.text = item.label
                description.text = item.description
                description.isVisible = item.description != null

                radioButton.isVisible = item.type == ListSelectionItem.Type.RADIO
                radioButton.isChecked = item.value == viewModel.selectedItem

                icon.isVisible = item.type == ListSelectionItem.Type.ICON
                item.iconRes?.let { icon.setImageResource(it) }

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
