package com.example.fixflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "warranties",
    foreignKeys = @ForeignKey(
        entity = RepairJob.class,
        parentColumns = "jobId",
        childColumns = "jobId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("jobId")}
)
public class Warranty {

    @PrimaryKey(autoGenerate = true)
    public long warrantyId;

    @NonNull
    public String jobId;

    public long startDate;
    public long completionDate;
    public String warrantyDuration;
    public String warrantyTerms;
    public long warrantyExpiryDate;

    @Nullable
    public String partsReplaced;

    public double finalCost;

    public Warranty(long warrantyId, @NonNull String jobId, long startDate, long completionDate,
                    String warrantyDuration, String warrantyTerms, long warrantyExpiryDate,
                    @Nullable String partsReplaced, double finalCost) {
        this.warrantyId = warrantyId;
        this.jobId = jobId;
        this.startDate = startDate != 0 ? startDate : System.currentTimeMillis();
        this.completionDate = completionDate;
        this.warrantyDuration = warrantyDuration;
        this.warrantyTerms = warrantyTerms;
        this.warrantyExpiryDate = warrantyExpiryDate;
        this.partsReplaced = partsReplaced;
        this.finalCost = finalCost;
    }

    public boolean isValid() {
        return System.currentTimeMillis() <= warrantyExpiryDate;
    }
}
