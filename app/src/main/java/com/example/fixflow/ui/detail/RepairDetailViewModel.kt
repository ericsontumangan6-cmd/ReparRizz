package com.example.fixflow.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.fixflow.data.db.FixFlowDatabase
import com.example.fixflow.data.entity.Payment
import com.example.fixflow.data.entity.RepairEstimate
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.entity.RepairStatusHistory
import com.example.fixflow.data.entity.RepairTest
import com.example.fixflow.data.entity.Warranty
import com.example.fixflow.data.entity.WarrantyClaim
import com.example.fixflow.data.repository.RepairRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class RepairDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RepairRepository
    private val jobIdLiveData = MutableLiveData<String>()

    val job: LiveData<RepairJob?> = jobIdLiveData.switchMap { id ->
        repository.getJobById(id)
    }

    val historyList: LiveData<List<RepairStatusHistory>> = jobIdLiveData.switchMap { id ->
        repository.getHistoryForJob(id)
    }

    val estimatesList: LiveData<List<RepairEstimate>> = jobIdLiveData.switchMap { id ->
        repository.getEstimatesForJob(id)
    }

    val testsList: LiveData<List<RepairTest>> = jobIdLiveData.switchMap { id ->
        repository.getTestsForJob(id)
    }

    val paymentsList: LiveData<List<Payment>> = jobIdLiveData.switchMap { id ->
        repository.getPaymentsForJob(id)
    }

    val totalRepairPaid: LiveData<Double> = jobIdLiveData.switchMap { id ->
        repository.getTotalRepairPaidForJob(id)
    }

    val totalTips: LiveData<Double> = jobIdLiveData.switchMap { id ->
        repository.getTotalTipsForJob(id)
    }

    val totalCashReceived: LiveData<Double> = jobIdLiveData.switchMap { id ->
        repository.getTotalCashReceivedForJob(id)
    }

    val warranty: LiveData<Warranty?> = jobIdLiveData.switchMap { id ->
        repository.getWarrantyForJob(id)
    }

    val claimsList: LiveData<List<WarrantyClaim>> = jobIdLiveData.switchMap { id ->
        repository.getClaimsForJob(id)
    }

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
    }

    fun setJobId(id: String) {
        if (jobIdLiveData.value != id) {
            jobIdLiveData.value = id
        }
    }

    fun updateAfterPhoto(photoPath: String?) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            repository.updateAfterPhoto(currentJobId, photoPath)
        }
    }

    fun saveDiagnosis(
        diagnosisNotes: String,
        suspectedFault: String?,
        recommendedRepair: String?,
        partsCost: Double,
        laborCost: Double,
        additionalCharges: Double
    ) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            repository.saveDiagnosisAndEstimate(
                currentJobId,
                diagnosisNotes,
                suspectedFault,
                recommendedRepair,
                partsCost,
                laborCost,
                additionalCharges
            )
        }
    }

    fun recordCustomerDecision(approved: Boolean, contactMethod: String?, notes: String?) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            repository.recordCustomerDecision(currentJobId, approved, contactMethod, notes)
        }
    }

    fun updateStatus(
        newStatus: String,
        staffNotes: String?,
        customerApproved: Boolean? = null,
        rejectionReason: String? = null,
        testingPassed: Boolean? = null
    ) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            repository.updateJobStatus(
                currentJobId,
                newStatus,
                staffNotes,
                customerApproved,
                rejectionReason,
                testingPassed
            )
        }
    }

    fun saveTests(tests: List<RepairTest>) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            repository.saveTestingChecklist(currentJobId, tests)
        }
    }

    fun recordPayment(repairAmount: Double, tipAmount: Double, method: String, notes: String?) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            repository.recordPayment(currentJobId, repairAmount, tipAmount, method, notes)
        }
    }

    fun releaseDevice(notes: String?) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            repository.recordDeviceRelease(currentJobId, notes)
        }
    }

    fun saveWarranty(duration: String, terms: String) {
        val currentJobId = jobIdLiveData.value ?: return
        val currentJob = job.value ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.timeInMillis = now

            when (duration) {
                "30 Days" -> cal.add(Calendar.DAY_OF_YEAR, 30)
                "60 Days" -> cal.add(Calendar.DAY_OF_YEAR, 60)
                "90 Days" -> cal.add(Calendar.DAY_OF_YEAR, 90)
                "180 Days (6 Months)" -> cal.add(Calendar.DAY_OF_YEAR, 180)
                "1 Year" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.DAY_OF_YEAR, 30)
            }

            val newWarranty = Warranty(
                0,
                currentJobId,
                now,
                now,
                duration,
                terms.trim(),
                cal.timeInMillis,
                currentJob.diagnosisNotes ?: "Replaced parts as diagnosed",
                if (currentJob.finalRepairCost > 0) currentJob.finalRepairCost else currentJob.totalEstimatedCost
            )
            repository.saveWarranty(newWarranty)
        }
    }

    fun createWarrantyClaim(reportedIssue: String, inspectionFindings: String?, status: String, decisionReason: String?) {
        val currentJobId = jobIdLiveData.value ?: return
        viewModelScope.launch {
            val claim = WarrantyClaim(
                0,
                currentJobId,
                reportedIssue.trim(),
                inspectionFindings?.trim(),
                status,
                decisionReason?.trim(),
                System.currentTimeMillis()
            )
            repository.createWarrantyClaim(claim)
        }
    }
}
