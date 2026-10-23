package com.example.fixflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "repair_estimates",
    foreignKeys = @ForeignKey(
        entity = RepairJob.class,
        parentColumns = "jobId",
        childColumns = "jobId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("jobId")}
)
public class RepairEstimate {

    @PrimaryKey(autoGenerate = true)
    public long estimateId;

    @NonNull
    public String jobId;

    public int estimateVersion; // 1, 2, 3...
    public double partsCost;
    public double laborCost;
    public double additionalCharges;
    public double estimatedTotal;

    @NonNull
    public String approvalStatus; // "PENDING", "APPROVED", "REJECTED"

    @Nullable
    public String contactMethod; // "In Person", "Phone Call", "SMS / Chat", "Email"

    public long approvalTimestamp;

    @Nullable
    public String staffNotes;

    public RepairEstimate(long estimateId, @NonNull String jobId, int estimateVersion,
                          double partsCost, double laborCost, double additionalCharges,
                          double estimatedTotal, @NonNull String approvalStatus,
                          @Nullable String contactMethod, long approvalTimestamp,
                          @Nullable String staffNotes) {
        this.estimateId = estimateId;
        this.jobId = jobId;
        this.estimateVersion = estimateVersion > 0 ? estimateVersion : 1;
        this.partsCost = partsCost;
        this.laborCost = laborCost;
        this.additionalCharges = additionalCharges;
        this.estimatedTotal = estimatedTotal;
        this.approvalStatus = approvalStatus != null ? approvalStatus : "PENDING";
        this.contactMethod = contactMethod;
        this.approvalTimestamp = approvalTimestamp;
        this.staffNotes = staffNotes;
    }
}
