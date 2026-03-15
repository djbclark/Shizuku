package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.ui.components.StyledBottomSheet
import moe.shizuku.manager.databinding.RadioButtonListItemBinding

class SelectionBottomSheet<T>(
    context: Context,
    @get:StringRes override val titleRes: Int? = null,
    @get:StringRes override val footerRes: Int? = null,
    private val items: List<SelectionItem<T>>,
    private val onConfirm: (T) -> Unit
) : StyledBottomSheet(context) {

    private var selectedValue: T? = null
    private val adapter = RadioButtonAdapter()

    data class SelectionItem<T>(
        val value: T,
        val label: CharSequence,
        val description: CharSequence? = null,
        val isEnabled: Boolean = true
    )

    override val contentView =
        RecyclerView(context).apply {
            id = android.R.id.list
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(context)
            adapter = this@SelectionBottomSheet.adapter
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

    fun show(currentValue: T) {
        selectedValue = currentValue
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
        super.show()
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
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = items.size
    }

    private inner class ViewHolder(private val binding: RadioButtonListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectionItem<T>) {
            binding.apply {
                title.text = item.label
                description.text = item.description
                description.isVisible = item.description != null

                radioButton.isChecked = item.value == selectedValue
                root.apply {
                    isEnabled = item.isEnabled
                    alpha = if (item.isEnabled) 1f else 0.38f
                    setOnClickListener {
                        if (item.isEnabled) {
                            onConfirm(item.value)
                            dismiss()
                        }
                    }
                }
            }
        }
    }
}
