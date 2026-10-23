package com.example.fixflow.ui.jobs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.databinding.ItemRepairJobBinding
import com.example.fixflow.util.StatusColorHelper
import com.example.fixflow.util.setOnSingleClickListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RepairJobAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<RepairJob, RepairJobAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class ViewHolder(val binding: ItemRepairJobBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRepairJobBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val job = getItem(position) ?: return
        val status = job.currentStatus ?: RepairJob.STATUS_RECEIVED
        val statusColor = StatusColorHelper.getColorForStatus(status)

        holder.binding.tvJobId.text = job.jobId
        holder.binding.tvStatusBadge.text = status
        holder.binding.tvStatusBadge.setBackgroundColor(statusColor)

        holder.binding.tvCustomerName.text = String.format(Locale.getDefault(), "%s (%s)", job.customerName ?: "Customer", job.contactNumber ?: "")
        holder.binding.tvDeviceInfo.text = String.format(Locale.getDefault(), "%s • %s %s", job.deviceType ?: "", job.deviceBrand ?: "", job.deviceModel ?: "")
        holder.binding.tvReportedIssue.text = String.format(Locale.getDefault(), "Issue: %s", job.reportedIssue ?: "N/A")
        holder.binding.tvJobDate.text = String.format(Locale.getDefault(), "Registered: %s", dateFormat.format(Date(job.createdAt)))

        val totalEst = job.totalEstimatedCost
        if (totalEst > 0) {
            holder.binding.tvJobCost.text = String.format(Locale.US, "Est: ₱%.2f", totalEst)
        } else {
            holder.binding.tvJobCost.text = "Est: Pending"
        }

        holder.itemView.setOnSingleClickListener {
            onItemClick(job.jobId)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<RepairJob>() {
        override fun areItemsTheSame(oldItem: RepairJob, newItem: RepairJob): Boolean {
            return oldItem.jobId == newItem.jobId
        }

        override fun areContentsTheSame(oldItem: RepairJob, newItem: RepairJob): Boolean {
            return oldItem.jobId == newItem.jobId &&
                    oldItem.currentStatus == newItem.currentStatus &&
                    oldItem.updatedAt == newItem.updatedAt
        }
    }
}
