package com.example.fixflow.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.entity.RepairStatusHistory
import com.example.fixflow.data.entity.Warranty
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateServiceReportPdf(
        context: Context,
        job: RepairJob,
        historyList: List<RepairStatusHistory>,
        warranty: Warranty?
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        var y = 50f

        // Title Header Background
        paint.color = Color.parseColor("#1B2230")
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Brand Title
        paint.color = Color.parseColor("#35D6C3")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("REPARIZZ SERVICE REPORT & WARRANTY", 30f, 50f, paint)

        // Subtitle / Tagline
        paint.color = Color.parseColor("#A7B3C4")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Giving broken gadgets their rizz back.", 30f, 75f, paint)

        y = 130f

        // Job Header Info Card
        drawSectionHeader(canvas, paint, "1. REPAIR JOB DETAILS", y)
        y += 25f

        drawLabelValue(canvas, paint, "Job ID:", job.jobId, 30f, y)
        drawLabelValue(canvas, paint, "Current Status:", job.currentStatus, 320f, y)
        y += 20f

        drawLabelValue(canvas, paint, "Date Registered:", dateFormat.format(Date(job.createdAt)), 30f, y)
        drawLabelValue(canvas, paint, "Customer Name:", job.customerName, 320f, y)
        y += 20f

        drawLabelValue(canvas, paint, "Contact Number:", job.contactNumber, 30f, y)
        y += 35f

        // Device Information
        drawSectionHeader(canvas, paint, "2. DEVICE SPECIFICATIONS", y)
        y += 25f

        drawLabelValue(canvas, paint, "Device Type:", job.deviceType, 30f, y)
        drawLabelValue(canvas, paint, "Brand & Model:", "${job.deviceBrand} ${job.deviceModel}", 320f, y)
        y += 20f

        drawLabelValue(canvas, paint, "Reported Problem:", job.reportedIssue, 30f, y)
        y += 20f

        drawLabelValue(canvas, paint, "Physical Condition:", job.deviceCondition, 30f, y)
        drawLabelValue(canvas, paint, "Accessories:", job.accessoriesReceived ?: "None", 320f, y)
        y += 35f

        // Diagnosis & Cost Breakdown
        drawSectionHeader(canvas, paint, "3. DIAGNOSIS & COST ESTIMATE", y)
        y += 25f

        drawLabelValue(canvas, paint, "Technician Findings:", job.diagnosisNotes ?: "N/A", 30f, y)
        y += 20f

        drawLabelValue(canvas, paint, "Estimated Parts:", "PHP ${String.format(Locale.US, "%.2f", job.estimatedPartsCost)}", 30f, y)
        drawLabelValue(canvas, paint, "Estimated Labor:", "PHP ${String.format(Locale.US, "%.2f", job.estimatedLaborCost)}", 320f, y)
        y += 20f

        paint.color = Color.BLACK
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val costDisplay = if (job.finalRepairCost > 0) job.finalRepairCost else job.totalEstimatedCost
        canvas.drawText("Total Cost: PHP ${String.format(Locale.US, "%.2f", costDisplay)}", 30f, y, paint)

        val approvalText = when (job.customerApproved) {
            true -> "APPROVED"
            false -> "REJECTED (${job.rejectionReason ?: "No reason"})"
            null -> "PENDING"
        }
        canvas.drawText("Customer Decision: $approvalText", 320f, y, paint)
        y += 35f

        // Service History Timeline Summary
        drawSectionHeader(canvas, paint, "4. REPAIR VERIFICATION & HISTORY", y)
        y += 25f

        val testText = if (job.testingPassed) "PASSED ALL TESTS" else "PENDING / INCOMPLETE"
        drawLabelValue(canvas, paint, "Final Verification Testing:", testText, 30f, y)
        y += 20f

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.DKGRAY
        for (hist in historyList.takeLast(4)) {
            val dateStr = dateFormat.format(Date(hist.timestamp))
            val noteStr = if (!hist.staffNotes.isNullOrEmpty()) " - Note: ${hist.staffNotes}" else ""
            canvas.drawText("• [$dateStr] Status: ${hist.newStatus}$noteStr", 35f, y, paint)
            y += 15f
        }
        y += 20f

        // Digital Warranty Section
        drawSectionHeader(canvas, paint, "5. DIGITAL WARRANTY RECORD", y)
        y += 25f

        if (warranty != null) {
            drawLabelValue(canvas, paint, "Warranty Period:", warranty.warrantyDuration, 30f, y)
            drawLabelValue(canvas, paint, "Warranty Expiry Date:", dateOnlyFormat.format(Date(warranty.warrantyExpiryDate)), 320f, y)
            y += 20f

            drawLabelValue(canvas, paint, "Parts Covered:", warranty.partsReplaced ?: "All Replaced Parts", 30f, y)
            y += 20f

            drawLabelValue(canvas, paint, "Warranty Terms:", warranty.warrantyTerms, 30f, y)
            y += 35f
        } else {
            drawLabelValue(canvas, paint, "Warranty Status:", "No warranty registered yet.", 30f, y)
            y += 35f
        }

        // Footer Sign-off
        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated by RepaRizz Digital Repair System. Currency: PHP (₱).", 30f, 810f, paint)

        pdfDocument.finishPage(page)

        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "RepaRizz_Report_${job.jobId}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun viewPdfFile(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            val chooser = Intent.createChooser(intent, "View Service Report PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_LONG).show()
        }
    }

    fun downloadPdfFile(context: Context, pdfFile: File): String? {
        return try {
            val fileName = pdfFile.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RepaRizz")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    "Downloads/RepaRizz/$fileName"
                } else null
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val repaRizzDir = File(downloadsDir, "RepaRizz")
                if (!repaRizzDir.exists()) repaRizzDir.mkdirs()
                val targetFile = File(repaRizzDir, fileName)
                pdfFile.copyTo(targetFile, overwrite = true)
                "Downloads/RepaRizz/$fileName"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Service Report & Warranty PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun drawSectionHeader(canvas: Canvas, paint: Paint, text: String, y: Float) {
        paint.color = Color.parseColor("#1B2230")
        canvas.drawRect(30f, y - 14f, 565f, y + 6f, paint)

        paint.color = Color.parseColor("#35D6C3")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(text, 35f, y, paint)
    }

    private fun drawLabelValue(canvas: Canvas, paint: Paint, label: String, value: String, x: Float, y: Float) {
        paint.color = Color.DKGRAY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(label, x, y, paint)

        val labelWidth = paint.measureText("$label ")
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(value, x + labelWidth, y, paint)
    }
}
