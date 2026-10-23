package com.example.fixflow.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.fixflow.data.db.FixFlowDatabase
import com.example.fixflow.data.entity.RepairStatusHistory
import com.example.fixflow.data.repository.RepairRepository

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RepairRepository

    val totalJobsCount: LiveData<Int>
    val awaitingDiagnosisCount: LiveData<Int>
    val awaitingApprovalCount: LiveData<Int>
    val inRepairCount: LiveData<Int>
    val failedTestingCount: LiveData<Int>
    val completedCount: LiveData<Int>
    val awaitingCollectionCount: LiveData<Int>
    val overdueActiveCount: LiveData<Int>
    val recentActivities: LiveData<List<RepairStatusHistory>>

    init {
        val db = FixFlowDatabase.getDatabase(application)
        repository = RepairRepository(
            db.repairJobDao(),
            db.repairEstimateDao(),
            db.repairStatusHistoryDao(),
            db.repairTestDao(),
            db.paymentDao(),
            db.warrantyDao(),
            db.warrantyClaimDao()
        )
        totalJobsCount = repository.getTotalJobsCount()
        awaitingDiagnosisCount = repository.getAwaitingDiagnosisCount()
        awaitingApprovalCount = repository.getAwaitingApprovalCount()
        inRepairCount = repository.getInRepairCount()
        failedTestingCount = repository.getFailedTestingCount()
        completedCount = repository.getCompletedCount()
        awaitingCollectionCount = repository.getAwaitingCollectionCount()
        overdueActiveCount = repository.getOverdueActiveCount()
        recentActivities = repository.getRecentHistory(10)
    }
}
