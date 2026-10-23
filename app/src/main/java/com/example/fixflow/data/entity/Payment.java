package com.example.fixflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "payments",
    foreignKeys = @ForeignKey(
        entity = RepairJob.class,
        parentColumns = "jobId",
        childColumns = "jobId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("jobId")}
)
public class Payment {

    @PrimaryKey(autoGenerate = true)
    public long paymentId;

    @NonNull
    public String jobId;

    public double repairAmount;
    public double tipAmount;
    public double amount; // total received = repairAmount + tipAmount

    @NonNull
    public String paymentMethod; // "Cash", "GCash", "Credit/Debit Card", "Bank Transfer", "Other"

    public long paymentTimestamp;

    @Nullable
    public String notes;

    public Payment(long paymentId, @NonNull String jobId, double repairAmount, double tipAmount,
                   double amount, @NonNull String paymentMethod, long paymentTimestamp, @Nullable String notes) {
        this.paymentId = paymentId;
        this.jobId = jobId;
        this.repairAmount = repairAmount;
        this.tipAmount = tipAmount;
        this.amount = amount != 0 ? amount : (repairAmount + tipAmount);
        this.paymentMethod = paymentMethod != null ? paymentMethod : "Cash";
        this.paymentTimestamp = paymentTimestamp != 0 ? paymentTimestamp : System.currentTimeMillis();
        this.notes = notes;
    }
}
