package com.example.fixflow.data.repository

import androidx.lifecycle.LiveData
import com.example.fixflow.data.dao.PaymentDao
import com.example.fixflow.data.dao.RepairEstimateDao
import com.example.fixflow.data.dao.RepairJobDao
import com.example.fixflow.data.dao.RepairStatusHistoryDao
import com.example.fixflow.data.dao.RepairTestDao
import com.example.fixflow.data.dao.WarrantyClaimDao
import com.example.fixflow.data.dao.WarrantyDao
import com.example.fixflow.data.entity.Payment
import com.example.fixflow.data.entity.RepairEstimate
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.entity.RepairStatusHistory
import com.example.fixflow.data.entity.RepairTest
import com.example.fixflow.data.entity.Warranty
import com.example.fixflow.data.entity.WarrantyClaim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

class RepairRepository(
    private val repairJobDao: RepairJobDao,
    private val estimateDao: RepairEstimateDao,
    private val historyDao: RepairStatusHistoryDao,
    private val testDao: RepairTestDao,
    private val paymentDao: PaymentDao,
    private val warrantyDao: WarrantyDao,
    private val claimDao: WarrantyClaimDao
) {

    fun getAllJobs(): LiveData<List<RepairJob>> = repairJobDao.allJobs

    fun getJobById(jobId: String): LiveData<RepairJob?> = repairJobDao.getJobById(jobId)

    fun searchAndFilterJobs(query: String?, status: String?): LiveData<List<RepairJob>> {
        val cleanQuery = query?.trim()?.ifEmpty { "" } ?: ""
        val cleanStatus = if (status.isNullOrEmpty()) "All" else status
        return repairJobDao.searchAndFilterJobs(cleanQuery, cleanStatus, System.currentTimeMillis())
    }

    fun getHistoryForJob(jobId: String): LiveData<List<RepairStatusHistory>> = historyDao.getHistoryForJob(jobId)

    fun getEstimatesForJob(jobId: String): LiveData<List<RepairEstimate>> = estimateDao.getEstimatesForJob(jobId)

    fun getLatestEstimate(jobId: String): LiveData<RepairEstimate> = estimateDao.getLatestEstimate(jobId)

    fun getTestsForJob(jobId: String): LiveData<List<RepairTest>> = testDao.getTestsForJob(jobId)

    fun getPaymentsForJob(jobId: String): LiveData<List<Payment>> = paymentDao.getPaymentsForJob(jobId)

    fun getTotalJobsCount(): LiveData<Int> = repairJobDao.totalJobsCount

    fun getTotalRepairPaidForJob(jobId: String): LiveData<Double> = paymentDao.getTotalRepairPaidForJob(jobId)

    fun getTotalTipsForJob(jobId: String): LiveData<Double> = paymentDao.getTotalTipsForJob(jobId)

    fun getTotalCashReceivedForJob(jobId: String): LiveData<Double> = paymentDao.getTotalCashReceivedForJob(jobId)

    fun getWarrantyForJob(jobId: String): LiveData<Warranty?> = warrantyDao.getWarrantyForJob(jobId)

    fun getClaimsForJob(jobId: String): LiveData<List<WarrantyClaim>> = claimDao.getClaimsForJob(jobId)

    fun getAllClaims(): LiveData<List<WarrantyClaim>> = claimDao.allClaims

    fun getRecentHistory(limit: Int = 10): LiveData<List<RepairStatusHistory>> = historyDao.getRecentHistory(limit)

    // Dashboard metrics
    fun getAwaitingDiagnosisCount(): LiveData<Int> = repairJobDao.awaitingDiagnosisCount
    fun getAwaitingApprovalCount(): LiveData<Int> = repairJobDao.awaitingApprovalJobsCount
    fun getInRepairCount(): LiveData<Int> = repairJobDao.inRepairJobsCount
    fun getFailedTestingCount(): LiveData<Int> = repairJobDao.failedTestingCount
    fun getCompletedCount(): LiveData<Int> = repairJobDao.completedJobsCount
    fun getAwaitingCollectionCount(): LiveData<Int> = repairJobDao.awaitingCollectionCount
    fun getOverdueActiveCount(): LiveData<Int> = repairJobDao.getOverdueActiveJobsCount(System.currentTimeMillis())

    suspend fun createNewRepairJob(job: RepairJob) = withContext(Dispatchers.IO) {
        repairJobDao.insertJob(job)
        val initialHistory = RepairStatusHistory(
            0,
            job.jobId,
            null,
            job.currentStatus,
            System.currentTimeMillis(),
            "Device intake registered."
        )
        historyDao.insertHistory(initialHistory)
    }

    suspend fun updateAfterPhoto(jobId: String, photoPath: String?) = withContext(Dispatchers.IO) {
        val currentJob = repairJobDao.getJobByIdSync(jobId) ?: return@withContext
        currentJob.afterPhotoPath = photoPath
        currentJob.updatedAt = System.currentTimeMillis()
        repairJobDao.updateJob(currentJob)

        val historyEntry = RepairStatusHistory(
            0,
            jobId,
            currentJob.currentStatus,
            currentJob.currentStatus,
            System.currentTimeMillis(),
            "Attached after-repair completion/release proof photo."
        )
        historyDao.insertHistory(historyEntry)
    }

    suspend fun saveDiagnosisAndEstimate(
        jobId: String,
        diagnosisNotes: String,
        suspectedFault: String?,
        recommendedRepair: String?,
        partsCost: Double,
        laborCost: Double,
        additionalCharges: Double
    ) = withContext(Dispatchers.IO) {
        val currentJob = repairJobDao.getJobByIdSync(jobId) ?: return@withContext
        val newTotal = partsCost + laborCost + additionalCharges
        val oldTotal = currentJob.totalEstimatedCost

        currentJob.diagnosisNotes = diagnosisNotes
        currentJob.suspectedFault = suspectedFault
        currentJob.recommendedRepair = recommendedRepair
        currentJob.estimatedPartsCost = partsCost
        currentJob.estimatedLaborCost = laborCost
        currentJob.additionalCharges = additionalCharges
        currentJob.updatedAt = System.currentTimeMillis()

        if (currentJob.customerApproved == true && newTotal > oldTotal) {
            currentJob.customerApproved = false
            currentJob.currentStatus = RepairJob.STATUS_AWAITING_APPROVAL
            
            val latestEst = estimateDao.getLatestEstimateSync(jobId)
            val nextVersion = if (latestEst != null) latestEst.estimateVersion + 1 else 1

            val newEstimate = RepairEstimate(
                0,
                jobId,
                nextVersion,
                partsCost,
                laborCost,
                additionalCharges,
                newTotal,
                "PENDING",
                null,
                0,
                "Revised estimate requiring updated customer approval."
            )
            estimateDao.insertEstimate(newEstimate)

            val revHistory = RepairStatusHistory(
                0,
                jobId,
                currentJob.currentStatus,
                RepairJob.STATUS_AWAITING_APPROVAL,
                System.currentTimeMillis(),
                "Estimate revised to ₱${String.format(Locale.US, "%.2f", newTotal)}. Required customer re-approval."
            )
            historyDao.insertHistory(revHistory)
        } else if (currentJob.customerApproved == null) {
            val latestEst = estimateDao.getLatestEstimateSync(jobId)
            val nextVersion = if (latestEst != null) latestEst.estimateVersion + 1 else 1

            val newEstimate = RepairEstimate(
                0,
                jobId,
                nextVersion,
                partsCost,
                laborCost,
                additionalCharges,
                newTotal,
                "PENDING",
                null,
                0,
                "Initial repair estimate created."
            )
            estimateDao.insertEstimate(newEstimate)
        }

        repairJobDao.updateJob(currentJob)
    }

    suspend fun recordCustomerDecision(
        jobId: String,
        approved: Boolean,
        contactMethod: String?,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        val currentJob = repairJobDao.getJobByIdSync(jobId) ?: return@withContext
        val prevStatus = currentJob.currentStatus

        currentJob.customerApproved = approved
        if (approved) {
            currentJob.currentStatus = RepairJob.STATUS_REPAIRING
            currentJob.rejectionReason = null
        } else {
            currentJob.currentStatus = RepairJob.STATUS_REJECTED
            currentJob.rejectionReason = notes ?: "Customer declined repair cost."
        }
        currentJob.updatedAt = System.currentTimeMillis()

        repairJobDao.updateJob(currentJob)

        val latestEst = estimateDao.getLatestEstimateSync(jobId)
        if (latestEst != null) {
            latestEst.approvalStatus = if (approved) "APPROVED" else "REJECTED"
            latestEst.contactMethod = contactMethod
            latestEst.approvalTimestamp = System.currentTimeMillis()
            latestEst.staffNotes = notes
            estimateDao.insertEstimate(latestEst)
        }

        val decisionText = if (approved) "Staff recorded customer APPROVAL" else "Staff recorded customer REJECTION"
        val methodText = if (!contactMethod.isNullOrEmpty()) " via $contactMethod" else ""
        val historyEntry = RepairStatusHistory(
            0,
            jobId,
            prevStatus,
            currentJob.currentStatus,
            System.currentTimeMillis(),
            "$decisionText$methodText. Notes: ${notes ?: "None"}"
        )
        historyDao.insertHistory(historyEntry)
    }

    suspend fun updateJobStatus(
        jobId: String,
        newStatus: String,
        staffNotes: String?,
        customerApproved: Boolean? = null,
        rejectionReason: String? = null,
        testingPassed: Boolean? = null
    ) = withContext(Dispatchers.IO) {
        val currentJob = repairJobDao.getJobByIdSync(jobId) ?: return@withContext
        val prevStatus = currentJob.currentStatus

        if (customerApproved != null) {
            currentJob.customerApproved = customerApproved
        }
        if (rejectionReason != null) {
            currentJob.rejectionReason = rejectionReason
        }
        if (testingPassed != null) {
            currentJob.testingPassed = testingPassed
        }

        if (RepairJob.STATUS_AWAITING_COLLECTION.equals(newStatus)) {
            currentJob.collectionStatus = "AWAITING_COLLECTION"
        } else if (RepairJob.STATUS_RELEASED.equals(newStatus)) {
            currentJob.collectionStatus = "RELEASED"
            currentJob.releaseDate = System.currentTimeMillis()
        }

        currentJob.currentStatus = newStatus
        currentJob.updatedAt = System.currentTimeMillis()

        repairJobDao.updateJob(currentJob)

        val historyEntry = RepairStatusHistory(
            0,
            jobId,
            prevStatus,
            newStatus,
            System.currentTimeMillis(),
            staffNotes
        )
        historyDao.insertHistory(historyEntry)
    }

    suspend fun saveTestingChecklist(
        jobId: String,
        tests: List<RepairTest>
    ) = withContext(Dispatchers.IO) {
        testDao.deleteTestsForJob(jobId)
        testDao.insertAllTests(tests)

        val allPassed = tests.all { it.result == "PASSED" }
        val currentJob = repairJobDao.getJobByIdSync(jobId)
        if (currentJob != null) {
            currentJob.testingPassed = allPassed
            repairJobDao.updateJob(currentJob)
        }
    }

    suspend fun recordPayment(
        jobId: String,
        repairAmount: Double,
        tipAmount: Double,
        method: String,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        val totalCashReceived = repairAmount + tipAmount
        val payment = Payment(0, jobId, repairAmount, tipAmount, totalCashReceived, method, System.currentTimeMillis(), notes)
        paymentDao.insertPayment(payment)

        val totalRepairPaid = paymentDao.getTotalRepairPaidForJobSync(jobId) ?: 0.0
        val currentJob = repairJobDao.getJobByIdSync(jobId) ?: return@withContext

        val costToPay = if (currentJob.finalRepairCost > 0) currentJob.finalRepairCost else currentJob.totalEstimatedCost
        if (currentJob.finalRepairCost <= 0) {
            currentJob.finalRepairCost = costToPay
        }

        if (totalRepairPaid >= costToPay && costToPay > 0) {
            currentJob.paymentStatus = "FULLY_PAID"
        } else if (totalRepairPaid > 0) {
            currentJob.paymentStatus = "PARTIALLY_PAID"
        } else {
            currentJob.paymentStatus = "UNPAID"
        }

        repairJobDao.updateJob(currentJob)

        val remainingBal = Math.max(0.0, costToPay - totalRepairPaid)
        val tipNote = if (tipAmount > 0) " (includes ₱${String.format(Locale.US, "%.2f", tipAmount)} tip)" else ""
        val historyEntry = RepairStatusHistory(
            0,
            jobId,
            currentJob.currentStatus,
            currentJob.currentStatus,
            System.currentTimeMillis(),
            "Recorded payment of ₱${String.format(Locale.US, "%.2f", totalCashReceived)}$tipNote via $method. Remaining repair balance: ₱${String.format(Locale.US, "%.2f", remainingBal)}"
        )
        historyDao.insertHistory(historyEntry)
    }

    suspend fun recordDeviceRelease(jobId: String, staffNotes: String?) = withContext(Dispatchers.IO) {
        updateJobStatus(jobId, RepairJob.STATUS_RELEASED, "Device released to customer. $staffNotes".trim())
    }

    suspend fun saveWarranty(warranty: Warranty) = withContext(Dispatchers.IO) {
        warrantyDao.insertWarranty(warranty)
    }

    suspend fun createWarrantyClaim(claim: WarrantyClaim) = withContext(Dispatchers.IO) {
        claimDao.insertClaim(claim)

        val historyEntry = RepairStatusHistory(
            0,
            claim.originalJobId,
            null,
            "Warranty Claim Registered",
            System.currentTimeMillis(),
            "Warranty claim logged: ${claim.reportedIssue}"
        )
        historyDao.insertHistory(historyEntry)
    }

    suspend fun updateWarrantyClaim(claim: WarrantyClaim) = withContext(Dispatchers.IO) {
        claimDao.updateClaim(claim)
    }

    suspend fun generateNextJobId(): String = withContext(Dispatchers.IO) {
        val count = repairJobDao.allJobsSync.size + 1
        return@withContext String.format(Locale.US, "FF-%04d", count)
    }

    data class AnalyticsSummary(
        val totalJobs: Int,
        val awaitingDiagnosis: Int,
        val awaitingApproval: Int,
        val currentlyRepairing: Int,
        val failedTesting: Int,
        val completedJobs: Int,
        val awaitingCollection: Int,
        val overdueActiveJobs: Int,
        val avgTurnaroundTimeHours: Double,
        val totalRepairRevenue: Double,
        val totalCustomerTips: Double,
        val totalCashReceived: Double,
        val totalOutstandingBalances: Double,
        val totalWarrantyClaims: Int,
        val topProblemTypes: List<Pair<String, Int>>
    )

    suspend fun calculateAnalytics(): AnalyticsSummary = withContext(Dispatchers.IO) {
        val allJobs = repairJobDao.allJobsSync
        val allHistory = historyDao.allHistorySync
        val allClaims = claimDao.allClaimsSync

        val totalActiveJobs = allJobs.count { it.currentStatus != RepairJob.STATUS_RELEASED && it.currentStatus != RepairJob.STATUS_REJECTED && it.currentStatus != RepairJob.STATUS_CANCELLED }
        val awaitingDiagnosis = allJobs.count { it.currentStatus == RepairJob.STATUS_DIAGNOSING || it.currentStatus == RepairJob.STATUS_RECEIVED }
        val awaitingApproval = allJobs.count { it.currentStatus == RepairJob.STATUS_AWAITING_APPROVAL }
        val currentlyRepairing = allJobs.count { it.currentStatus == RepairJob.STATUS_REPAIRING }
        val failedTesting = allJobs.count { it.currentStatus == RepairJob.STATUS_TESTING && !it.testingPassed }
        val completedJobs = allJobs.count { it.currentStatus == RepairJob.STATUS_COMPLETED || it.currentStatus == RepairJob.STATUS_AWAITING_COLLECTION || it.currentStatus == RepairJob.STATUS_RELEASED }
        val awaitingCollection = allJobs.count { it.currentStatus == RepairJob.STATUS_AWAITING_COLLECTION || it.collectionStatus == "AWAITING_COLLECTION" }
        val overdueActiveJobs = allJobs.count { it.isOverdue }

        var totalTurnaroundMs = 0L
        var completedCountWithHistory = 0

        val historyByJob = allHistory.groupBy { it.jobId }
        for (job in allJobs) {
            if (RepairJob.STATUS_COMPLETED.equals(job.currentStatus) || RepairJob.STATUS_RELEASED.equals(job.currentStatus)) {
                val histories = historyByJob[job.jobId] ?: emptyList()
                val received = histories.find { RepairJob.STATUS_RECEIVED.equals(it.newStatus) } ?: histories.firstOrNull()
                val completed = histories.find { RepairJob.STATUS_COMPLETED.equals(it.newStatus) } ?: histories.lastOrNull()
                if (received != null && completed != null && completed.timestamp >= received.timestamp) {
                    totalTurnaroundMs += (completed.timestamp - received.timestamp)
                    completedCountWithHistory++
                }
            }
        }

        val avgHours = if (completedCountWithHistory > 0) {
            TimeUnit.MILLISECONDS.toHours(totalTurnaroundMs / completedCountWithHistory).toDouble()
        } else {
            0.0
        }

        val totalRepairRevenue = paymentDao.totalRepairRevenueAllSync ?: 0.0
        val totalCustomerTips = paymentDao.totalTipsAllSync ?: 0.0
        val totalCashReceived = paymentDao.totalCashAllSync ?: 0.0

        var totalOutstandingBalances = 0.0
        for (job in allJobs) {
            if (!RepairJob.STATUS_CANCELLED.equals(job.currentStatus) && !RepairJob.STATUS_REJECTED.equals(job.currentStatus)) {
                val cost = if (job.finalRepairCost > 0) job.finalRepairCost else job.totalEstimatedCost
                val paidRepair = paymentDao.getTotalRepairPaidForJobSync(job.jobId) ?: 0.0
                val bal = Math.max(0.0, cost - paidRepair)
                if (bal > 0) {
                    totalOutstandingBalances += bal
                }
            }
        }

        val problemCounts = allJobs.map { it.reportedIssue.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        return@withContext AnalyticsSummary(
            totalJobs = totalActiveJobs,
            awaitingDiagnosis = awaitingDiagnosis,
            awaitingApproval = awaitingApproval,
            currentlyRepairing = currentlyRepairing,
            failedTesting = failedTesting,
            completedJobs = completedJobs,
            awaitingCollection = awaitingCollection,
            overdueActiveJobs = overdueActiveJobs,
            avgTurnaroundTimeHours = avgHours,
            totalRepairRevenue = totalRepairRevenue,
            totalCustomerTips = totalCustomerTips,
            totalCashReceived = totalCashReceived,
            totalOutstandingBalances = totalOutstandingBalances,
            totalWarrantyClaims = allClaims.size,
            topProblemTypes = problemCounts
        )
    }
}
