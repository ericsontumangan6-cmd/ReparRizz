package com.example.fixflow.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.entity.RepairStatusHistory
import com.example.fixflow.databinding.ItemRecentActivityBinding
import com.example.fixflow.util.StatusColorHelper
import com.example.fixflow.util.setOnSingleClickListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentActivityAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<RepairStatusHistory, RecentActivityAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    class ViewHolder(val binding: ItemRecentActivityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val status = item.newStatus ?: RepairJob.STATUS_RECEIVED
        val statusColor = StatusColorHelper.getColorForStatus(status)

        holder.binding.viewStatusIndicator.setBackgroundColor(statusColor)
        holder.binding.tvActivityTitle.text = String.format(Locale.getDefault(), "Job %s: Status changed to %s", item.jobId, status)

        val notes = item.staffNotes
        if (!notes.isNullOrEmpty()) {
            holder.binding.tvActivityNotes.text = notes
            holder.binding.tvActivityNotes.visibility = View.VISIBLE
        } else {
            holder.binding.tvActivityNotes.visibility = View.GONE
        }

        holder.binding.tvActivityTime.text = dateFormat.format(Date(item.timestamp))

        holder.itemView.setOnSingleClickListener {
            onItemClick(item.jobId)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<RepairStatusHistory>() {
        override fun areItemsTheSame(oldItem: RepairStatusHistory, newItem: RepairStatusHistory): Boolean {
            return oldItem.historyId == newItem.historyId
        }

        override fun areContentsTheSame(oldItem: RepairStatusHistory, newItem: RepairStatusHistory): Boolean {
            return oldItem.historyId == newItem.historyId &&
                    oldItem.newStatus == newItem.newStatus &&
                    oldItem.staffNotes == newItem.staffNotes
        }
    }
}
