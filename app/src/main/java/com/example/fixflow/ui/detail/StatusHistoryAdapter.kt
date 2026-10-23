package com.example.fixflow.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.entity.RepairStatusHistory
import com.example.fixflow.databinding.ItemStatusHistoryBinding
import com.example.fixflow.util.StatusColorHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusHistoryAdapter : ListAdapter<RepairStatusHistory, StatusHistoryAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    class ViewHolder(val binding: ItemStatusHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatusHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val status = item.newStatus ?: RepairJob.STATUS_RECEIVED
        val statusColor = StatusColorHelper.getColorForStatus(status)

        holder.binding.vNodeDot.setBackgroundColor(statusColor)
        holder.binding.tvHistoryStatus.text = status
        holder.binding.tvHistoryTime.text = dateFormat.format(Date(item.timestamp))

        val notes = item.staffNotes
        if (!notes.isNullOrEmpty()) {
            holder.binding.tvHistoryNotes.text = String.format(Locale.getDefault(), "Staff note: %s", notes)
            holder.binding.tvHistoryNotes.visibility = View.VISIBLE
        } else {
            holder.binding.tvHistoryNotes.visibility = View.GONE
        }

        if (position == itemCount - 1) {
            holder.binding.vTimelineLine.visibility = View.INVISIBLE
        } else {
            holder.binding.vTimelineLine.visibility = View.VISIBLE
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
