package moe.shizuku.manager.core.ui.components.listselection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.RadioButtonListItemBinding

class ListSelectionAdapter(
    private val onItemClick: (ListSelectionItem) -> Unit
) : RecyclerView.Adapter<ListSelectionAdapter.ViewHolder>() {

    var items: List<ListSelectionItem> = emptyList()
        set(value) {
            field = value
            notifyItemRangeChanged(0, value.size)
        }

    var selectedItem: Any? = null
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.radio_button_list_item, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding by viewBinding(RadioButtonListItemBinding::bind)

        fun bind(item: ListSelectionItem) {
            setLabel(item)
            setDescription(item)
            setLeadingIcon(item)
            setTrailingIcon(item)
            setEnabled(item)
            setOnClick(item)
        }

        private fun setLabel(item: ListSelectionItem) = with(binding) {
            val labelText = item.label ?: if (item.labelRes != 0) {
                itemView.context.getString(item.labelRes)
            } else null
            title.text = labelText
        }

        private fun setDescription(item: ListSelectionItem) = with(binding) {
            val descriptionText = item.description ?: item.descriptionRes?.let {
                itemView.context.getString(it)
            }
            description.text = descriptionText
            description.isVisible = descriptionText != null
        }

        private fun setLeadingIcon(item: ListSelectionItem) = with(binding) {
            icon.isVisible = item.iconRes != null
            item.iconRes?.let { icon.setImageResource(it) }
        }

        private fun setTrailingIcon(item: ListSelectionItem) = with(binding) {
            radioButton.isVisible = item.type == ListSelectionItem.Type.RADIO
            radioButton.isChecked = item == selectedItem

            trailingIcon.isVisible = item.type == ListSelectionItem.Type.LINK
        }

        private fun setEnabled(item: ListSelectionItem) = with(binding) {
            root.isEnabled = item.isEnabled
            root.alpha = if (item.isEnabled) 1f else 0.38f
        }

        private fun setOnClick(item: ListSelectionItem) =
            binding.root.setOnClickListener {
                if (item.isEnabled) {
                    onItemClick(item)
                }
            }

    }
}
