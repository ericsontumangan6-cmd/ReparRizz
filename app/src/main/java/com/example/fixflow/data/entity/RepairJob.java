package com.example.fixflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Arrays;
import java.util.List;

@Entity(tableName = "repair_jobs")
public class RepairJob {

    public static final String STATUS_RECEIVED = "Received";
    public static final String STATUS_DIAGNOSING = "Diagnosing";
    public static final String STATUS_AWAITING_APPROVAL = "Awaiting Approval";
    public static final String STATUS_REPAIRING = "Repairing";
    public static final String STATUS_TESTING = "Testing";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_AWAITING_COLLECTION = "Awaiting Collection";
    public static final String STATUS_RELEASED = "Released";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_CANCELLED = "Cancelled";

    public static final List<String> ALL_STATUSES = Arrays.asList(
            STATUS_RECEIVED, STATUS_DIAGNOSING, STATUS_AWAITING_APPROVAL,
            STATUS_REPAIRING, STATUS_TESTING, STATUS_COMPLETED,
            STATUS_AWAITING_COLLECTION, STATUS_RELEASED, STATUS_REJECTED, STATUS_CANCELLED
    );

    @PrimaryKey
    @NonNull
    public String jobId;
    public String customerName;
    public String contactNumber;
    public String deviceType;
    public String deviceBrand;
    public String deviceModel;
    public String reportedIssue;
    public String deviceCondition;
    @Nullable public String accessoriesReceived;
    @Nullable public String photoPath; // intake / condition photo
    @Nullable public String beforePhotoPath;
    @Nullable public String afterPhotoPath;
    @Nullable public String diagnosisNotes;
    @Nullable public String suspectedFault;
    @Nullable public String recommendedRepair;
    public double estimatedPartsCost;
    public double estimatedLaborCost;
    public double additionalCharges;
    @Nullable public Boolean customerApproved;
    @Nullable public String rejectionReason;
    public String currentStatus;
    public boolean testingPassed;
    public long expectedCompletionDate;
    public double finalRepairCost;
    public String paymentStatus; // "UNPAID", "PARTIALLY_PAID", "FULLY_PAID"
    public String collectionStatus; // "IN_SHOP", "AWAITING_COLLECTION", "RELEASED"
    public long releaseDate;
    public long createdAt;
    public long updatedAt;

    public RepairJob(@NonNull String jobId, String customerName, String contactNumber,
                     String deviceType, String deviceBrand, String deviceModel,
                     String reportedIssue, String deviceCondition, @Nullable String accessoriesReceived,
                     @Nullable String photoPath, @Nullable String beforePhotoPath, @Nullable String afterPhotoPath,
                     @Nullable String diagnosisNotes, @Nullable String suspectedFault, @Nullable String recommendedRepair,
                     double estimatedPartsCost, double estimatedLaborCost, double additionalCharges,
                     @Nullable Boolean customerApproved, @Nullable String rejectionReason,
                     String currentStatus, boolean testingPassed, long expectedCompletionDate,
                     double finalRepairCost, String paymentStatus, String collectionStatus, long releaseDate,
                     long createdAt, long updatedAt) {
        this.jobId = jobId;
        this.customerName = customerName;
        this.contactNumber = contactNumber;
        this.deviceType = deviceType;
        this.deviceBrand = deviceBrand;
        this.deviceModel = deviceModel;
        this.reportedIssue = reportedIssue;
        this.deviceCondition = deviceCondition;
        this.accessoriesReceived = accessoriesReceived;
        this.photoPath = photoPath;
        this.beforePhotoPath = beforePhotoPath;
        this.afterPhotoPath = afterPhotoPath;
        this.diagnosisNotes = diagnosisNotes;
        this.suspectedFault = suspectedFault;
        this.recommendedRepair = recommendedRepair;
        this.estimatedPartsCost = estimatedPartsCost;
        this.estimatedLaborCost = estimatedLaborCost;
        this.additionalCharges = additionalCharges;
        this.customerApproved = customerApproved;
        this.rejectionReason = rejectionReason;
        this.currentStatus = currentStatus != null ? currentStatus : STATUS_RECEIVED;
        this.testingPassed = testingPassed;
        this.expectedCompletionDate = expectedCompletionDate;
        this.finalRepairCost = finalRepairCost;
        this.paymentStatus = paymentStatus != null ? paymentStatus : "UNPAID";
        this.collectionStatus = collectionStatus != null ? collectionStatus : "IN_SHOP";
        this.releaseDate = releaseDate;
        this.createdAt = createdAt != 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt != 0 ? updatedAt : System.currentTimeMillis();
    }

    public double getTotalEstimatedCost() {
        return estimatedPartsCost + estimatedLaborCost + additionalCharges;
    }

    public boolean isOverdue() {
        if (expectedCompletionDate <= 0) return false;
        if (STATUS_RELEASED.equals(currentStatus) || STATUS_REJECTED.equals(currentStatus) || STATUS_CANCELLED.equals(currentStatus)) {
            return false;
        }
        return System.currentTimeMillis() > expectedCompletionDate;
    }
}
