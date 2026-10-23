package com.example.fixflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "repair_status_history",
    foreignKeys = @ForeignKey(
        entity = RepairJob.class,
        parentColumns = "jobId",
        childColumns = "jobId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("jobId")}
)
public class RepairStatusHistory {

    @PrimaryKey(autoGenerate = true)
    public long historyId;

    @NonNull
    public String jobId;

    @Nullable
    public String previousStatus;

    @NonNull
    public String newStatus;

    public long timestamp;

    @Nullable
    public String staffNotes;

    public RepairStatusHistory(long historyId, @NonNull String jobId, @Nullable String previousStatus,
                               @NonNull String newStatus, long timestamp, @Nullable String staffNotes) {
        this.historyId = historyId;
        this.jobId = jobId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.timestamp = timestamp != 0 ? timestamp : System.currentTimeMillis();
        this.staffNotes = staffNotes;
    }
}
