package moe.shizuku.manager.privilegedservice.ui.start

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.utils.runnable.RunnableStatus
import moe.shizuku.manager.databinding.ItemStartStepBinding
import moe.shizuku.manager.privilegedservice.models.StartStepItem

class StartStepAdapter :
    ListAdapter<StartStepItem, StartStepAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemStartStepBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemStartStepBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StartStepItem) {
            binding.label.setText(item.label)
            binding.icon.setImageResource(item.icon)

            val status = item.status
            binding.statusPending.isVisible = status is RunnableStatus.Pending
            binding.statusRunning.isVisible = status is RunnableStatus.Running
            binding.statusCompleted.isVisible = status is RunnableStatus.Completed
            binding.statusFailed.isVisible = status is RunnableStatus.Failed
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StartStepItem>() {
        override fun areItemsTheSame(
            oldItem: StartStepItem,
            newItem: StartStepItem
        ): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(
            oldItem: StartStepItem,
            newItem: StartStepItem
        ): Boolean {
            return oldItem.status == newItem.status
        }
    }
}
