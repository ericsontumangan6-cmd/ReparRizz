package com.example.fixflow.util

import androidx.core.graphics.toColorInt
import com.example.fixflow.data.entity.RepairJob

object StatusColorHelper {

    val RECEIVED_COLOR = "#4A90E2".toColorInt()
    val DIAGNOSING_COLOR = "#F5A623".toColorInt()
    val AWAITING_APPROVAL_COLOR = "#FF9800".toColorInt()
    val REPAIRING_COLOR = "#35D6C3".toColorInt()
    val TESTING_COLOR = "#9C27B0".toColorInt()
    val COMPLETED_COLOR = "#4CAF50".toColorInt()
    val AWAITING_COLLECTION_COLOR = "#00BCD4".toColorInt()
    val RELEASED_COLOR = "#8BC34A".toColorInt()
    val REJECTED_COLOR = "#E53935".toColorInt()
    val CANCELLED_COLOR = "#757575".toColorInt()

    fun getColorForStatus(status: String?): Int {
        return when (status) {
            RepairJob.STATUS_RECEIVED -> RECEIVED_COLOR
            RepairJob.STATUS_DIAGNOSING -> DIAGNOSING_COLOR
            RepairJob.STATUS_AWAITING_APPROVAL -> AWAITING_APPROVAL_COLOR
            RepairJob.STATUS_REPAIRING -> REPAIRING_COLOR
            RepairJob.STATUS_TESTING -> TESTING_COLOR
            RepairJob.STATUS_COMPLETED -> COMPLETED_COLOR
            RepairJob.STATUS_AWAITING_COLLECTION -> AWAITING_COLLECTION_COLOR
            RepairJob.STATUS_RELEASED -> RELEASED_COLOR
            RepairJob.STATUS_REJECTED -> REJECTED_COLOR
            RepairJob.STATUS_CANCELLED -> CANCELLED_COLOR
            else -> REPAIRING_COLOR
        }
    }
}
