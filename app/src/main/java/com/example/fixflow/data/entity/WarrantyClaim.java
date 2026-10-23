package com.example.fixflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "warranty_claims",
    foreignKeys = @ForeignKey(
        entity = RepairJob.class,
        parentColumns = "jobId",
        childColumns = "originalJobId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("originalJobId")}
)
public class WarrantyClaim {

    @PrimaryKey(autoGenerate = true)
    public long claimId;

    @NonNull
    public String originalJobId;

    @NonNull
    public String reportedIssue;

    @Nullable
    public String inspectionFindings;

    @NonNull
    public String claimStatus; // "PENDING_INSPECTION", "APPROVED", "REJECTED"

    @Nullable
    public String decisionReason;

    public long timestamp;

    public WarrantyClaim(long claimId, @NonNull String originalJobId, @NonNull String reportedIssue,
                         @Nullable String inspectionFindings, @NonNull String claimStatus,
                         @Nullable String decisionReason, long timestamp) {
        this.claimId = claimId;
        this.originalJobId = originalJobId;
        this.reportedIssue = reportedIssue;
        this.inspectionFindings = inspectionFindings;
        this.claimStatus = claimStatus != null ? claimStatus : "PENDING_INSPECTION";
        this.decisionReason = decisionReason;
        this.timestamp = timestamp != 0 ? timestamp : System.currentTimeMillis();
    }
}
