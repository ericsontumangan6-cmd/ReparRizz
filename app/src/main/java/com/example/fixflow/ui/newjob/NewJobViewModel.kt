package com.example.fixflow.ui.newjob

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fixflow.data.db.FixFlowDatabase
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.repository.RepairRepository
import kotlinx.coroutines.launch

class NewJobViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RepairRepository

    private val _generatedJobId = MutableLiveData<String>()
    val generatedJobId: LiveData<String> = _generatedJobId

    private val _saveSuccessEvent = MutableLiveData<String?>()
    val saveSuccessEvent: LiveData<String?> = _saveSuccessEvent

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
        fetchNextJobId()
    }

    fun fetchNextJobId() {
        viewModelScope.launch {
            _generatedJobId.value = repository.generateNextJobId()
        }
    }

    fun createJob(
        customerName: String,
        contactNumber: String,
        deviceType: String,
        deviceBrand: String,
        deviceModel: String,
        reportedIssue: String,
        deviceCondition: String,
        accessoriesReceived: String?,
        beforePhotoPath: String?,
        expectedCompletionDate: Long
    ) {
        viewModelScope.launch {
            val jobId = _generatedJobId.value ?: repository.generateNextJobId()
            val now = System.currentTimeMillis()
            val newJob = RepairJob(
                jobId,
                customerName.trim(),
                contactNumber.trim(),
                deviceType.ifEmpty { "Smartphone" },
                deviceBrand.trim(),
                deviceModel.trim(),
                reportedIssue.trim(),
                deviceCondition.trim(),
                accessoriesReceived?.trim(),
                beforePhotoPath, // photoPath / intake
                beforePhotoPath,
                null, // afterPhotoPath
                null, // diagnosisNotes
                null, // suspectedFault
                null, // recommendedRepair
                0.0,  // partsCost
                0.0,  // laborCost
                0.0,  // additionalCharges
                null, // customerApproved
                null, // rejectionReason
                RepairJob.STATUS_RECEIVED,
                false,
                expectedCompletionDate,
                0.0,  // finalRepairCost
                "UNPAID",
                "IN_SHOP",
                0,
                now,
                now
            )
            repository.createNewRepairJob(newJob)
            _saveSuccessEvent.value = jobId
        }
    }

    fun resetSaveEvent() {
        _saveSuccessEvent.value = null
    }
}
