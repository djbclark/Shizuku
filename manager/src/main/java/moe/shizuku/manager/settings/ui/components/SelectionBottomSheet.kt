package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.ui.components.BaseBottomSheet
import moe.shizuku.manager.databinding.RadioButtonListItemBinding
import moe.shizuku.manager.databinding.SelectionBottomSheetBinding

class SelectionBottomSheet<T>(
    context: Context,
    @get:StringRes private val titleRes: Int,
    @get:StringRes private val footerRes: Int? = null,
    private val entries: List<T>,
    private val itemMapper: (T) -> SelectionItem,
    private val onConfirm: (T) -> Unit
) : BaseBottomSheet(context) {

    private val binding = SelectionBottomSheetBinding.inflate(LayoutInflater.from(context))
    private var selectedValue: T? = null
    private val adapter = RadioButtonAdapter()

    data class SelectionItem(
        val label: CharSequence,
        val description: CharSequence? = null,
        val isEnabled: Boolean = true
    )

    init {
        title = titleRes
        setContentView(binding.root)

        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SelectionBottomSheet.adapter
        }

        binding.apply {
            footer.isVisible = footerRes != null
            footerRes?.let { footerText.setText(it) }
        }
    }

    fun show(currentValue: T) {
        selectedValue = currentValue
        adapter.notifyItemRangeChanged(0, adapter.getItemCount())
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
            val entry = entries[position]
            holder.bind(entry)
        }

        override fun getItemCount(): Int = entries.size
    }

    private inner class ViewHolder(private val binding: RadioButtonListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: T) {
            val item = itemMapper(entry)

            binding.apply {
                title.text = item.label
                description.text = item.description
                description.isVisible = item.description != null

                radioButton.isChecked = entry == selectedValue
                root.apply {
                    isEnabled = item.isEnabled
                    alpha = if (item.isEnabled) 1f else 0.38f
                    setOnClickListener {
                        if (item.isEnabled) {
                            onConfirm(entry)
                            dismiss()
                        }
                    }
                }
            }
        }

    }

}
