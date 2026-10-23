package com.example.fixflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "repair_tests",
    foreignKeys = @ForeignKey(
        entity = RepairJob.class,
        parentColumns = "jobId",
        childColumns = "jobId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("jobId")}
)
public class RepairTest {

    @PrimaryKey(autoGenerate = true)
    public long testId;

    @NonNull
    public String jobId;

    @NonNull
    public String testName; // e.g., "Display & Touchscreen", "Charging", "Speaker & Microphone", "Cameras", "Buttons", "Connectivity"

    @NonNull
    public String result; // "PASSED", "FAILED", "PENDING"

    @Nullable
    public String technicianNotes;

    public long timestamp;

    public RepairTest(long testId, @NonNull String jobId, @NonNull String testName,
                      @NonNull String result, @Nullable String technicianNotes, long timestamp) {
        this.testId = testId;
        this.jobId = jobId;
        this.testName = testName;
        this.result = result != null ? result : "PENDING";
        this.technicianNotes = technicianNotes;
        this.timestamp = timestamp != 0 ? timestamp : System.currentTimeMillis();
    }
}
