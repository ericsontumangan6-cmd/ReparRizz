package com.example.fixflow

import com.example.fixflow.data.entity.RepairJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairJobTest {

    @Test
    fun testTotalEstimatedCostCalculation() {
        val now = System.currentTimeMillis()
        val job = RepairJob(
            "FF-0001",
            "John Doe",
            "09123456789",
            "Smartphone",
            "Apple",
            "iPhone 13",
            "Cracked Screen",
            "Minor scratches",
            "Charger",
            null,
            null,
            null,
            "Replaced screen assembly",
            "Cracked LCD",
            "Screen replacement",
            150.0,
            50.0,
            20.0,
            null,
            null,
            RepairJob.STATUS_RECEIVED,
            false,
            now + 86400000L,
            220.0,
            "UNPAID",
            "IN_SHOP",
            0,
            now,
            now
        )

        assertEquals(220.0, job.totalEstimatedCost, 0.001)
        assertEquals(RepairJob.STATUS_RECEIVED, job.currentStatus)
        assertNull(job.customerApproved)
        assertFalse(job.isOverdue)
    }

    @Test
    fun testCustomerApprovalAndOverdueStates() {
        val now = System.currentTimeMillis()
        val pastDeadline = now - 3600000L

        val overdueJob = RepairJob(
            "FF-0002",
            "Jane Smith",
            "09987654321",
            "Laptop",
            "Dell",
            "XPS 15",
            "Battery replacement",
            "Good",
            "Case",
            null,
            null,
            null,
            null,
            null,
            null,
            100.0,
            50.0,
            0.0,
            true,
            null,
            RepairJob.STATUS_REPAIRING,
            false,
            pastDeadline,
            150.0,
            "UNPAID",
            "IN_SHOP",
            0,
            now,
            now
        )

        assertTrue(overdueJob.customerApproved == true)
        assertEquals(RepairJob.STATUS_REPAIRING, overdueJob.currentStatus)
        assertTrue(overdueJob.isOverdue)
    }
}
