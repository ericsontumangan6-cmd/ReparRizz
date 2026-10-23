package com.example.fixflow.ui.detail

import android.content.DialogInterface
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fixflow.R
import com.example.fixflow.data.entity.Payment
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.entity.RepairStatusHistory
import com.example.fixflow.data.entity.RepairTest
import com.example.fixflow.databinding.FragmentRepairDetailBinding
import com.example.fixflow.databinding.ItemStatusHistoryBinding
import com.example.fixflow.util.ImageUtils
import com.example.fixflow.util.PdfReportGenerator
import com.example.fixflow.util.StatusColorHelper
import com.example.fixflow.util.setOnSingleClickListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RepairDetailFragment : Fragment() {

    private var _binding: FragmentRepairDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RepairDetailViewModel by viewModels()

    private var currentJob: RepairJob? = null
    private val currentTestChecklist = mutableListOf<RepairTest>()
    private var selectedAfterPhotoUri: Uri? = null

    private val pickAfterPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAfterPhotoUri = uri
            ImageUtils.loadScaledImageAsync(binding.ivAfterPhotoPreview, uri.toString(), binding.layoutAfterPhotoBox)

            val job = currentJob
            if (job != null) {
                job.afterPhotoPath = uri.toString()
                viewModel.updateAfterPhoto(uri.toString())
                Toast.makeText(requireContext(), "After-repair proof photo saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepairDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val jobId = arguments?.getString("jobId")
        if (jobId == null) {
            Toast.makeText(requireContext(), "Error: Invalid Repair Job ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        viewModel.setJobId(jobId)

        setupWarrantyDropdown()
        observeViewModel()
        setupListeners()
    }

    private fun setupWarrantyDropdown() {
        val durations = arrayOf("30 Days", "60 Days", "90 Days", "180 Days (6 Months)", "1 Year")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, durations)
        binding.actvWarrantyDuration.setAdapter(adapter)
        binding.actvWarrantyDuration.setText(durations[2], false)

        binding.etWarrantyTerms.setText(getString(R.string.hint_warranty_terms))
    }

    private fun observeViewModel() {
        viewModel.job.observe(viewLifecycleOwner) { job ->
            if (job == null) return@observe
            currentJob = job
            bindJobData(job)
        }

        viewModel.historyList.observe(viewLifecycleOwner) { historyList ->
            updateAuditHistoryTimeline(historyList)
        }

        viewModel.testsList.observe(viewLifecycleOwner) { savedTests ->
            setupTestingChecklist(savedTests)
        }

        viewModel.paymentsList.observe(viewLifecycleOwner) { paymentsList ->
            updatePaymentAndCollectionSummary()
            updatePaymentHistoryList(paymentsList)
        }

        viewModel.totalRepairPaid.observe(viewLifecycleOwner) { _ ->
            updatePaymentAndCollectionSummary()
        }

        viewModel.totalTips.observe(viewLifecycleOwner) { _ ->
            updatePaymentAndCollectionSummary()
        }

        viewModel.totalCashReceived.observe(viewLifecycleOwner) { _ ->
            updatePaymentAndCollectionSummary()
        }

        viewModel.warranty.observe(viewLifecycleOwner) { warranty ->
            if (warranty != null) {
                binding.actvWarrantyDuration.setText(warranty.warrantyDuration, false)
                binding.etWarrantyTerms.setText(warranty.warrantyTerms)
            }
        }
    }

    private fun bindJobData(job: RepairJob) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

        binding.tvHeaderJobId.text = String.format(Locale.getDefault(), "Job Record: %s", job.jobId)
        binding.tvHeaderCustomer.text = String.format(Locale.getDefault(), "%s • %s", job.customerName, job.contactNumber)

        val statusColor = StatusColorHelper.getColorForStatus(job.currentStatus)

        binding.tvDetailStatusBadge.text = job.currentStatus
        binding.tvDetailStatusBadge.setBackgroundColor(statusColor)

        if (job.isOverdue) {
            binding.tvOverdueBadge.visibility = View.VISIBLE
        } else {
            binding.tvOverdueBadge.visibility = View.GONE
        }

        binding.tvDetailCustomerInfo.text = String.format(Locale.getDefault(), "Customer: %s (%s)", job.customerName, job.contactNumber)
        binding.tvDetailDeviceInfo.text = String.format(Locale.getDefault(), "Device: %s • %s %s", job.deviceType, job.deviceBrand, job.deviceModel)
        binding.tvDetailReportedIssue.text = String.format(Locale.getDefault(), "Reported Problem: %s", job.reportedIssue)
        binding.tvDetailDeviceCondition.text = String.format(Locale.getDefault(), "Physical Condition: %s", job.deviceCondition)
        binding.tvDetailAccessories.text = String.format(Locale.getDefault(), "Accessories Received: %s", job.accessoriesReceived ?: "None")

        val deadlineStr = if (job.expectedCompletionDate > 0) dateFormat.format(Date(job.expectedCompletionDate)) else "Not set"
        binding.tvDetailDeadline.text = String.format(Locale.getDefault(), "Target Completion: %s", deadlineStr)

        val beforePhoto = job.beforePhotoPath ?: job.photoPath
        ImageUtils.loadScaledImageAsync(binding.ivDetailPhoto, beforePhoto, binding.layoutBeforePhotoBox)

        val afterPhoto = job.afterPhotoPath ?: selectedAfterPhotoUri?.toString()
        ImageUtils.loadScaledImageAsync(binding.ivAfterPhotoPreview, afterPhoto, binding.layoutAfterPhotoBox)

        updateVisualStageStepper(job.currentStatus ?: RepairJob.STATUS_RECEIVED)

        // Diagnosis section
        if (binding.etDiagnosisNotes.text.isNullOrEmpty()) {
            binding.etDiagnosisNotes.setText(job.diagnosisNotes ?: "")
        }
        if (binding.etPartsCost.text.isNullOrEmpty() && job.estimatedPartsCost > 0) {
            binding.etPartsCost.setText(job.estimatedPartsCost.toString())
        }
        if (binding.etLaborCost.text.isNullOrEmpty() && job.estimatedLaborCost > 0) {
            binding.etLaborCost.setText(job.estimatedLaborCost.toString())
        }
        if (binding.etAdditionalCost.text.isNullOrEmpty() && job.additionalCharges > 0) {
            binding.etAdditionalCost.setText(job.additionalCharges.toString())
        }
        updateTotalEstimateDisplay()

        // Customer Decision Status Display
        when (job.customerApproved) {
            true -> {
                binding.tvApprovalStatusDesc.text = "Status: APPROVED by customer (Recorded by Staff)"
                binding.tvApprovalStatusDesc.setTextColor("#4CAF50".toColorInt())
                binding.layoutApprovalButtons.visibility = View.GONE
            }
            false -> {
                binding.tvApprovalStatusDesc.text = String.format(Locale.getDefault(), "Status: REJECTED by customer (%s)", job.rejectionReason ?: "Reason unspecified")
                binding.tvApprovalStatusDesc.setTextColor("#E53935".toColorInt())
                binding.layoutApprovalButtons.visibility = View.GONE
            }
            null -> {
                binding.tvApprovalStatusDesc.text = String.format(Locale.US, "Status: Pending customer response on estimate (₱%.2f)", job.totalEstimatedCost)
                binding.tvApprovalStatusDesc.setTextColor("#FF9800".toColorInt())
                if (job.currentStatus == RepairJob.STATUS_AWAITING_APPROVAL || job.currentStatus == RepairJob.STATUS_DIAGNOSING) {
                    binding.layoutApprovalButtons.visibility = View.VISIBLE
                } else {
                    binding.layoutApprovalButtons.visibility = View.GONE
                }
            }
        }

        // Timeline Progress Button styling and logic
        when (job.currentStatus) {
            RepairJob.STATUS_RECEIVED -> {
                binding.btnNextStatusAction.text = "Begin Diagnosis → Diagnosing"
                binding.btnNextStatusAction.isEnabled = true
            }
            RepairJob.STATUS_DIAGNOSING -> {
                binding.btnNextStatusAction.text = "Submit Estimate → Awaiting Approval"
                binding.btnNextStatusAction.isEnabled = true
            }
            RepairJob.STATUS_AWAITING_APPROVAL -> {
                if (job.customerApproved == true) {
                    binding.btnNextStatusAction.text = "Start Repair Work → Repairing"
                    binding.btnNextStatusAction.isEnabled = true
                } else {
                    binding.btnNextStatusAction.text = "Awaiting Customer Response Record"
                    binding.btnNextStatusAction.isEnabled = false
                }
            }
            RepairJob.STATUS_REPAIRING -> {
                binding.btnNextStatusAction.text = "Finish Repair Work → Start Testing"
                binding.btnNextStatusAction.isEnabled = true
            }
            RepairJob.STATUS_TESTING -> {
                if (job.testingPassed) {
                    binding.btnNextStatusAction.text = "All Tests Passed → Complete Repair"
                    binding.btnNextStatusAction.isEnabled = true
                } else {
                    binding.btnNextStatusAction.text = "Perform & Pass Final Testing Checklist"
                    binding.btnNextStatusAction.isEnabled = true
                }
            }
            RepairJob.STATUS_COMPLETED -> {
                binding.btnNextStatusAction.text = "Ready for Customer Collection → Awaiting Collection"
                binding.btnNextStatusAction.isEnabled = true
            }
            RepairJob.STATUS_AWAITING_COLLECTION -> {
                binding.btnNextStatusAction.text = "Release Device to Customer → Released"
                binding.btnNextStatusAction.isEnabled = true
            }
            RepairJob.STATUS_RELEASED -> {
                binding.btnNextStatusAction.text = "✓ Device Released to Customer"
                binding.btnNextStatusAction.isEnabled = false
            }
            RepairJob.STATUS_REJECTED -> {
                binding.btnNextStatusAction.text = "✗ Job Rejected & Closed"
                binding.btnNextStatusAction.isEnabled = false
            }
            RepairJob.STATUS_CANCELLED -> {
                binding.btnNextStatusAction.text = "🚫 Job Cancelled"
                binding.btnNextStatusAction.isEnabled = false
            }
        }

        updatePaymentAndCollectionSummary()
    }

    private fun updateVisualStageStepper(currentStatus: String) {
        binding.layoutStageTimelineStepper.removeAllViews()

        val context = requireContext()
        val stages = listOf(
            RepairJob.STATUS_RECEIVED,
            RepairJob.STATUS_DIAGNOSING,
            RepairJob.STATUS_AWAITING_APPROVAL,
            RepairJob.STATUS_REPAIRING,
            RepairJob.STATUS_TESTING,
            RepairJob.STATUS_COMPLETED,
            RepairJob.STATUS_AWAITING_COLLECTION,
            RepairJob.STATUS_RELEASED
        )

        val currentStageIndex = stages.indexOf(currentStatus).let { if (it < 0) 0 else it }

        for ((index, stageName) in stages.withIndex()) {
            val stepRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 6, 0, 6)
            }

            val badgeTv = TextView(context).apply {
                val isPassed = index < currentStageIndex
                val isCurrent = index == currentStageIndex

                text = when {
                    isPassed -> "✓"
                    isCurrent -> "▶"
                    else -> "${index + 1}"
                }
                textSize = 11f
                setPadding(14, 6, 14, 6)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_status_badge)

                when {
                    isPassed -> setBackgroundColor("#4CAF50".toColorInt())
                    isCurrent -> setBackgroundColor("#35D6C3".toColorInt())
                    else -> setBackgroundColor("#222C3D".toColorInt())
                }
                setTextColor(if (isCurrent) "#10141D".toColorInt() else "#FFFFFF".toColorInt())
            }

            val labelTv = TextView(context).apply {
                text = "Stage ${index + 1}: $stageName"
                textSize = 12f
                setPadding(16, 0, 0, 0)
                when {
                    index < currentStageIndex -> {
                        setTextColor("#4CAF50".toColorInt())
                        setTypeface(null, Typeface.NORMAL)
                    }
                    index == currentStageIndex -> {
                        setTextColor("#35D6C3".toColorInt())
                        setTypeface(null, Typeface.BOLD)
                    }
                    else -> {
                        setTextColor("#606E7F".toColorInt())
                        setTypeface(null, Typeface.NORMAL)
                    }
                }
            }

            stepRow.addView(badgeTv)
            stepRow.addView(labelTv)
            binding.layoutStageTimelineStepper.addView(stepRow)
        }
    }

    private fun updateAuditHistoryTimeline(historyList: List<RepairStatusHistory>?) {
        binding.layoutAuditHistoryContainer.removeAllViews()

        if (historyList.isNullOrEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No status change history recorded yet."
                setTextColor("#A7B3C4".toColorInt())
                textSize = 12f
                setPadding(0, 8, 0, 8)
            }
            binding.layoutAuditHistoryContainer.addView(emptyTv)
            return
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        val inflater = LayoutInflater.from(requireContext())

        for ((index, item) in historyList.withIndex()) {
            val rowBinding = ItemStatusHistoryBinding.inflate(inflater, binding.layoutAuditHistoryContainer, false)
            val status = item.newStatus ?: RepairJob.STATUS_RECEIVED
            val statusColor = StatusColorHelper.getColorForStatus(status)

            rowBinding.vNodeDot.setBackgroundColor(statusColor)
            rowBinding.tvHistoryStatus.text = status
            rowBinding.tvHistoryTime.text = dateFormat.format(Date(item.timestamp))

            val notes = item.staffNotes
            if (!notes.isNullOrEmpty()) {
                rowBinding.tvHistoryNotes.text = String.format(Locale.getDefault(), "Staff note: %s", notes)
                rowBinding.tvHistoryNotes.visibility = View.VISIBLE
            } else {
                rowBinding.tvHistoryNotes.visibility = View.GONE
            }

            if (index == historyList.size - 1) {
                rowBinding.vTimelineLine.visibility = View.GONE
            } else {
                rowBinding.vTimelineLine.visibility = View.VISIBLE
            }

            binding.layoutAuditHistoryContainer.addView(rowBinding.root)
        }
    }

    private fun setupTestingChecklist(savedTests: List<RepairTest>?) {
        val job = currentJob ?: return
        val defaultTestNames = when (job.deviceType) {
            "Smartphone", "Tablet" -> listOf("Display & Touchscreen", "Charging Port & Battery", "Speaker & Microphone", "Cameras (Front & Rear)", "Physical Buttons", "Wi-Fi & Cellular Connectivity")
            "Laptop", "Desktop" -> listOf("Display / Monitor Output", "Power & Charging", "Keyboard & Trackpad", "Audio Output & Mic", "USB Ports & IO", "Operating System Boot")
            "Smartwatch" -> listOf("Display & Touch", "Charging Dock", "Sensors & Vibration", "Physical Crown / Buttons", "Bluetooth Pairing")
            else -> listOf("Power & Basic Operation", "Physical Housing / Frame", "Main Functional Components", "Final Safety Check")
        }

        currentTestChecklist.clear()
        binding.layoutTestingChecklist.removeAllViews()

        val context = requireContext()
        val savedMap = savedTests?.associateBy { it.testName } ?: emptyMap()

        for (name in defaultTestNames) {
            val savedTest = savedMap[name]
            val result = savedTest?.result ?: "PENDING"
            val testObj = RepairTest(
                savedTest?.testId ?: 0,
                job.jobId,
                name,
                result,
                savedTest?.technicianNotes,
                System.currentTimeMillis()
            )
            currentTestChecklist.add(testObj)

            val checkRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }

            val cb = CheckBox(context).apply {
                text = name
                setTextColor("#FFFFFF".toColorInt())
                isChecked = (result == "PASSED")
                setOnCheckedChangeListener { _, isChecked ->
                    testObj.result = if (isChecked) "PASSED" else "FAILED"
                }
            }

            checkRow.addView(cb)
            binding.layoutTestingChecklist.addView(checkRow)
        }
    }

    private fun updatePaymentAndCollectionSummary() {
        val job = currentJob ?: return
        val paidRepair = viewModel.totalRepairPaid.value ?: 0.0
        val tips = viewModel.totalTips.value ?: 0.0
        val totalCash = viewModel.totalCashReceived.value ?: (paidRepair + tips)

        val cost = if (job.finalRepairCost > 0) job.finalRepairCost else job.totalEstimatedCost
        val balance = Math.max(0.0, cost - paidRepair)

        binding.tvPaymentStatusBadge.text = job.paymentStatus ?: "UNPAID"
        binding.tvCollectionStatusBadge.text = job.collectionStatus ?: "IN_SHOP"

        binding.tvFinalCostDisplay.text = String.format(Locale.US, "Final Repair Cost: ₱%.2f", cost)
        binding.tvRepairPaidDisplay.text = String.format(Locale.US, "Total Repair Payments: ₱%.2f", paidRepair)
        binding.tvTipsDisplay.text = String.format(Locale.US, "Customer Tips: ₱%.2f", tips)
        binding.tvTotalCashDisplay.text = String.format(Locale.US, "Total Cash Received: ₱%.2f", totalCash)

        binding.tvOutstandingBalanceDisplay.text = String.format(Locale.US, "Outstanding Balance: ₱%.2f", balance)

        if (balance <= 0 && cost > 0) {
            binding.tvOutstandingBalanceDisplay.setTextColor("#4CAF50".toColorInt())
        } else {
            binding.tvOutstandingBalanceDisplay.setTextColor("#E53935".toColorInt())
        }
    }

    private fun updatePaymentHistoryList(paymentsList: List<Payment>?) {
        binding.layoutPaymentHistoryContainer.removeAllViews()
        if (paymentsList.isNullOrEmpty()) return

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val context = requireContext()

        val headerTv = TextView(context).apply {
            text = "Payment Transaction Records:"
            setTextColor("#35D6C3".toColorInt())
            textSize = 12f
            setPadding(0, 12, 0, 4)
        }
        binding.layoutPaymentHistoryContainer.addView(headerTv)

        for (p in paymentsList) {
            val dateStr = dateFormat.format(Date(p.paymentTimestamp))
            val notesStr = if (!p.notes.isNullOrEmpty()) " (${p.notes})" else ""

            val rowTv = TextView(context).apply {
                val tipStr = if (p.tipAmount > 0) String.format(Locale.US, " + Tip ₱%.2f", p.tipAmount) else ""
                text = String.format(
                    Locale.US,
                    "• ₱%.2f via %s on %s%s%s",
                    p.repairAmount,
                    p.paymentMethod,
                    dateStr,
                    tipStr,
                    notesStr
                )
                setTextColor("#A7B3C4".toColorInt())
                textSize = 12f
                setPadding(8, 2, 0, 2)
            }
            binding.layoutPaymentHistoryContainer.addView(rowTv)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnSingleClickListener {
            findNavController().navigateUp()
        }

        val costWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTotalEstimateDisplay()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etPartsCost.addTextChangedListener(costWatcher)
        binding.etLaborCost.addTextChangedListener(costWatcher)
        binding.etAdditionalCost.addTextChangedListener(costWatcher)

        // Save Diagnosis & Estimate
        binding.btnSaveDiagnosis.setOnSingleClickListener {
            val notes = binding.etDiagnosisNotes.text?.toString()?.trim().orEmpty()
            val partsCost = binding.etPartsCost.text?.toString()?.toDoubleOrNull() ?: 0.0
            val laborCost = binding.etLaborCost.text?.toString()?.toDoubleOrNull() ?: 0.0
            val additional = binding.etAdditionalCost.text?.toString()?.toDoubleOrNull() ?: 0.0

            if (notes.isEmpty()) {
                binding.tilDiagnosisNotes.error = "Diagnosis findings required"
                return@setOnSingleClickListener
            }
            binding.tilDiagnosisNotes.error = null

            viewModel.saveDiagnosis(notes, null, null, partsCost, laborCost, additional)
            Toast.makeText(requireContext(), "Diagnosis & Estimate saved!", Toast.LENGTH_SHORT).show()
        }

        // Customer Response Buttons
        binding.btnRecordApproval.setOnSingleClickListener {
            showCustomerDecisionDialog(approved = true)
        }

        binding.btnRecordRejection.setOnSingleClickListener {
            showCustomerDecisionDialog(approved = false)
        }

        // Next Stage Progress Button
        binding.btnNextStatusAction.setOnSingleClickListener {
            val job = currentJob ?: return@setOnSingleClickListener

            when (job.currentStatus) {
                RepairJob.STATUS_RECEIVED -> {
                    showStaffNotesDialog("Notes for starting diagnosis") { notes ->
                        viewModel.updateStatus(RepairJob.STATUS_DIAGNOSING, notes)
                    }
                }
                RepairJob.STATUS_DIAGNOSING -> {
                    val notes = binding.etDiagnosisNotes.text?.toString()?.trim().orEmpty()
                    if (notes.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter diagnosis findings first!", Toast.LENGTH_SHORT).show()
                        return@setOnSingleClickListener
                    }
                    showStaffNotesDialog("Notes for submitting estimate") { staffNotes ->
                        viewModel.updateStatus(RepairJob.STATUS_AWAITING_APPROVAL, staffNotes)
                    }
                }
                RepairJob.STATUS_AWAITING_APPROVAL -> {
                    if (job.customerApproved == true) {
                        showStaffNotesDialog("Notes for starting repair") { staffNotes ->
                            viewModel.updateStatus(RepairJob.STATUS_REPAIRING, staffNotes)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Staff must record customer approval response first!", Toast.LENGTH_LONG).show()
                    }
                }
                RepairJob.STATUS_REPAIRING -> {
                    showStaffNotesDialog("Notes for finishing repair & starting testing") { staffNotes ->
                        viewModel.updateStatus(RepairJob.STATUS_TESTING, staffNotes)
                    }
                }
                RepairJob.STATUS_TESTING -> {
                    val allPassed = currentTestChecklist.all { it.result == "PASSED" }
                    if (!allPassed) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Testing Checklist Incomplete")
                            .setMessage("Some required device tests failed or are pending. Return job to Repairing stage for rework?")
                            .setPositiveButton("Return to Repairing") { _, _ ->
                                viewModel.updateStatus(
                                    RepairJob.STATUS_REPAIRING,
                                    "Testing checklist failed. Returned to Repairing stage for rework.",
                                    testingPassed = false
                                )
                                Toast.makeText(requireContext(), "Job returned to Repairing stage.", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        viewModel.updateStatus(
                            RepairJob.STATUS_COMPLETED,
                            "Passed all final testing checklist items.",
                            testingPassed = true
                        )
                        Toast.makeText(requireContext(), "Job marked as COMPLETED!", Toast.LENGTH_SHORT).show()
                    }
                }
                RepairJob.STATUS_COMPLETED -> {
                    viewModel.updateStatus(RepairJob.STATUS_AWAITING_COLLECTION, "Moved to Awaiting Collection stage.")
                }
                RepairJob.STATUS_AWAITING_COLLECTION -> {
                    viewModel.releaseDevice("Device released to customer.")
                    Toast.makeText(requireContext(), "Device marked as RELEASED!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Cancel Job
        binding.btnCancelJob.setOnSingleClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cancel Repair Job")
                .setMessage("Are you sure you want to cancel this repair job? This action will mark the job as Cancelled.")
                .setPositiveButton("Yes, Cancel Job") { _, _ ->
                    showStaffNotesDialog("Reason for cancellation") { notes ->
                        viewModel.updateStatus(RepairJob.STATUS_CANCELLED, "Job cancelled by staff: $notes")
                        Toast.makeText(requireContext(), "Job marked as Cancelled.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Save Testing Checklist
        binding.btnSaveTests.setOnSingleClickListener {
            viewModel.saveTests(currentTestChecklist)
            Toast.makeText(requireContext(), "Final testing results saved!", Toast.LENGTH_SHORT).show()
        }

        // Record Payment
        binding.btnRecordPayment.setOnSingleClickListener {
            showRecordPaymentDialog()
        }

        // Release Device
        binding.btnReleaseDevice.setOnSingleClickListener {
            showStaffNotesDialog("Device Release Notes") { notes ->
                viewModel.releaseDevice(notes)
                Toast.makeText(requireContext(), "Device marked as RELEASED!", Toast.LENGTH_SHORT).show()
            }
        }

        // Attach After-Repair Proof Photo
        binding.btnAttachAfterPhoto.setOnSingleClickListener {
            pickAfterPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // Save Warranty
        binding.btnSaveWarranty.setOnSingleClickListener {
            val duration = binding.actvWarrantyDuration.text.toString()
            val terms = binding.etWarrantyTerms.text?.toString()?.trim().orEmpty()

            if (terms.isEmpty()) {
                binding.tilWarrantyTerms.error = "Warranty terms required"
                return@setOnSingleClickListener
            }
            binding.tilWarrantyTerms.error = null

            viewModel.saveWarranty(duration, terms)
            Toast.makeText(requireContext(), "Warranty terms saved!", Toast.LENGTH_SHORT).show()
        }

        // File Warranty Claim
        binding.btnFileWarrantyClaim.setOnSingleClickListener {
            showFileWarrantyClaimDialog()
        }

        // View PDF
        binding.btnViewPdf.setOnSingleClickListener {
            val pdfFile = generatePdf() ?: return@setOnSingleClickListener
            PdfReportGenerator.viewPdfFile(requireContext(), pdfFile)
        }

        // Download PDF
        binding.btnDownloadPdf.setOnSingleClickListener {
            val pdfFile = generatePdf() ?: return@setOnSingleClickListener
            val savedPath = PdfReportGenerator.downloadPdfFile(requireContext(), pdfFile)
            if (savedPath != null) {
                Toast.makeText(requireContext(), "PDF downloaded: $savedPath", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Failed to download PDF.", Toast.LENGTH_SHORT).show()
            }
        }

        // Share PDF
        binding.btnSharePdf.setOnSingleClickListener {
            val pdfFile = generatePdf() ?: return@setOnSingleClickListener
            PdfReportGenerator.sharePdfFile(requireContext(), pdfFile)
        }

        // Finish Session & Return to Dashboard
        binding.btnFinishSession.setOnSingleClickListener {
            Toast.makeText(requireContext(), "Repair record session finished.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_dashboard)
        }
    }

    private fun showCustomerDecisionDialog(approved: Boolean) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        val tvMethodLabel = TextView(context).apply {
            text = "Contact Method Used:"
            setTextColor("#A7B3C4".toColorInt())
            textSize = 12f
        }

        val methods = arrayOf("In Person", "Phone Call", "SMS / Chat", "Email")
        val spinnerAdapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, methods) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? TextView)?.setTextColor("#FFFFFF".toColorInt())
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                v.setBackgroundColor("#1B2230".toColorInt())
                (v as? TextView)?.setTextColor("#FFFFFF".toColorInt())
                return v
            }
        }

        val spinnerMethod = Spinner(context).apply {
            adapter = spinnerAdapter
        }

        val etNotes = EditText(context).apply {
            hint = if (approved) "Notes (e.g., Customer agreed over phone call)..." else "Reason for rejection (e.g., Cost too high)..."
            setTextColor("#FFFFFF".toColorInt())
            setHintTextColor("#A7B3C4".toColorInt())
        }

        container.addView(tvMethodLabel)
        container.addView(spinnerMethod)
        container.addView(etNotes)

        val titleText = if (approved) "Record Customer APPROVAL" else "Record Customer REJECTION"

        AlertDialog.Builder(context)
            .setTitle(titleText)
            .setMessage("Confirm staff recording of customer's actual response:")
            .setView(container)
            .setPositiveButton("Save Response") { _, _ ->
                val method = spinnerMethod.selectedItem.toString()
                val notes = etNotes.text.toString().trim()
                viewModel.recordCustomerDecision(approved, method, notes)
                val statusText = if (approved) "APPROVED" else "REJECTED"
                Toast.makeText(context, "Recorded customer response: $statusText", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRecordPaymentDialog() {
        val context = requireContext()
        val job = currentJob ?: return

        val cost = if (job.finalRepairCost > 0) job.finalRepairCost else job.totalEstimatedCost
        val existingPaidRepair = viewModel.totalRepairPaid.value ?: 0.0
        val remainingBal = Math.max(0.0, cost - existingPaidRepair)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        val tvInfo = TextView(context).apply {
            text = String.format(Locale.US, "Remaining Balance Due: ₱%.2f", remainingBal)
            setTextColor("#35D6C3".toColorInt())
            textSize = 13f
            setPadding(0, 0, 0, 12)
        }

        val etRepairAmount = EditText(context).apply {
            hint = "Repair Payment Amount (₱) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor("#FFFFFF".toColorInt())
            setHintTextColor("#A7B3C4".toColorInt())
            if (remainingBal > 0) {
                setText(String.format(Locale.US, "%.2f", remainingBal))
            }
        }

        val etTipAmount = EditText(context).apply {
            hint = "Customer Tip (₱) [Optional]"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor("#FFFFFF".toColorInt())
            setHintTextColor("#A7B3C4".toColorInt())
        }

        val tvMethodLabel = TextView(context).apply {
            text = "Payment Method:"
            setTextColor("#35D6C3".toColorInt())
            textSize = 13f
            setPadding(0, 16, 0, 4)
        }

        val methods = arrayOf("Cash", "GCash", "Credit/Debit Card", "Bank Transfer", "Other")
        val spinnerAdapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, methods) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? TextView)?.setTextColor("#FFFFFF".toColorInt())
                (v as? TextView)?.textSize = 14f
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                v.setBackgroundColor("#1B2230".toColorInt())
                (v as? TextView)?.setTextColor("#FFFFFF".toColorInt())
                (v as? TextView)?.textSize = 14f
                return v
            }
        }

        val spinnerMethod = Spinner(context).apply {
            adapter = spinnerAdapter
            setBackgroundColor("#151C28".toColorInt())
            setPadding(16, 16, 16, 16)
        }

        val etNotes = EditText(context).apply {
            hint = "Payment Notes (Reference No, OR #)..."
            setTextColor("#FFFFFF".toColorInt())
            setHintTextColor("#A7B3C4".toColorInt())
        }

        container.addView(tvInfo)
        container.addView(etRepairAmount)
        container.addView(etTipAmount)
        container.addView(tvMethodLabel)
        container.addView(spinnerMethod)
        container.addView(etNotes)

        AlertDialog.Builder(context)
            .setTitle("Record Customer Payment & Tip")
            .setView(container)
            .setPositiveButton("Save Payment") { _, _ ->
                val enteredRepair = etRepairAmount.text.toString().toDoubleOrNull() ?: 0.0
                val enteredTip = etTipAmount.text.toString().toDoubleOrNull() ?: 0.0
                val method = spinnerMethod.selectedItem.toString()
                val notes = etNotes.text.toString().trim()

                if (enteredRepair <= 0 && enteredTip <= 0) {
                    Toast.makeText(context, "Enter a valid positive payment or tip amount.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (remainingBal > 0 && enteredRepair > remainingBal) {
                    val excess = enteredRepair - remainingBal
                    AlertDialog.Builder(context)
                        .setTitle("Excess Repair Payment Detected")
                        .setMessage(String.format(Locale.US, "The entered repair payment (₱%.2f) exceeds the remaining balance due (₱%.2f) by ₱%.2f.\n\nHow would you like to classify the excess ₱%.2f?", enteredRepair, remainingBal, excess, excess))
                        .setPositiveButton("Classify Excess as Tip") { _, _ ->
                            val finalRepairAmt = remainingBal
                            val finalTipAmt = enteredTip + excess
                            viewModel.recordPayment(finalRepairAmt, finalTipAmt, method, "$notes (₱${String.format(Locale.US, "%.2f", excess)} excess classified as Tip)".trim())
                            Toast.makeText(context, String.format(Locale.US, "Recorded repair payment ₱%.2f + Tip ₱%.2f via %s", finalRepairAmt, finalTipAmt, method), Toast.LENGTH_LONG).show()
                        }
                        .setNegativeButton("Keep as Repair Payment") { _, _ ->
                            viewModel.recordPayment(enteredRepair, enteredTip, method, notes)
                            Toast.makeText(context, String.format(Locale.US, "Recorded repair payment ₱%.2f + Tip ₱%.2f via %s", enteredRepair, enteredTip, method), Toast.LENGTH_LONG).show()
                        }
                        .show()
                } else {
                    viewModel.recordPayment(enteredRepair, enteredTip, method, notes)
                    Toast.makeText(context, String.format(Locale.US, "Recorded repair payment ₱%.2f + Tip ₱%.2f via %s", enteredRepair, enteredTip, method), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFileWarrantyClaimDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        val etIssue = EditText(context).apply {
            hint = "Claim Reported Issue (e.g., Screen flickering again) *"
            setTextColor("#FFFFFF".toColorInt())
            setHintTextColor("#A7B3C4".toColorInt())
        }

        val etFindings = EditText(context).apply {
            hint = "Inspection Findings (optional)..."
            setTextColor("#FFFFFF".toColorInt())
            setHintTextColor("#A7B3C4".toColorInt())
        }

        container.addView(etIssue)
        container.addView(etFindings)

        AlertDialog.Builder(context)
            .setTitle("File Warranty Claim")
            .setMessage("Register a new warranty claim for inspection:")
            .setView(container)
            .setPositiveButton("File Claim") { _, _ ->
                val issue = etIssue.text.toString().trim()
                if (issue.isEmpty()) {
                    Toast.makeText(context, "Reported issue is required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val findings = etFindings.text.toString().trim()
                viewModel.createWarrantyClaim(issue, findings, "PENDING_INSPECTION", "Registered for technician inspection.")
                Toast.makeText(context, "Warranty claim registered under PENDING_INSPECTION.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generatePdf(): File? {
        val job = currentJob
        if (job == null) {
            Toast.makeText(requireContext(), "No repair record available.", Toast.LENGTH_SHORT).show()
            return null
        }

        val pdfFile = PdfReportGenerator.generateServiceReportPdf(
            context = requireContext(),
            job = job,
            historyList = viewModel.historyList.value ?: emptyList(),
            warranty = viewModel.warranty.value
        )

        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(requireContext(), "Failed to generate PDF report.", Toast.LENGTH_SHORT).show()
            return null
        }
        return pdfFile
    }

    private fun updateTotalEstimateDisplay() {
        val partsCost = binding.etPartsCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        val laborCost = binding.etLaborCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        val additional = binding.etAdditionalCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        val total = partsCost + laborCost + additional
        binding.tvTotalEstimateDisplay.text = String.format(Locale.US, "Estimated Total: ₱%.2f", total)
    }

    private fun showStaffNotesDialog(title: String, onConfirm: (String) -> Unit) {
        val input = EditText(requireContext())
        input.hint = "Enter staff notes (optional)..."
        val container = LinearLayout(requireContext())
        container.setPadding(48, 16, 48, 16)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                val notes = input.text.toString().trim()
                onConfirm(notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
