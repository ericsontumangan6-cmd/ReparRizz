package com.example.fixflow.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fixflow.data.entity.RepairJob;

import java.util.List;

@Dao
public interface RepairJobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertJob(RepairJob job);

    @Update
    void updateJob(RepairJob job);

    @Query("SELECT * FROM repair_jobs WHERE jobId = :jobId LIMIT 1")
    LiveData<RepairJob> getJobById(String jobId);

    @Query("SELECT * FROM repair_jobs WHERE jobId = :jobId LIMIT 1")
    RepairJob getJobByIdSync(String jobId);

    @Query("SELECT * FROM repair_jobs ORDER BY createdAt DESC")
    LiveData<List<RepairJob>> getAllJobs();

    @Query("SELECT * FROM repair_jobs ORDER BY createdAt DESC")
    List<RepairJob> getAllJobsSync();

    @Query("SELECT * FROM repair_jobs WHERE " +
           "(:status IS NULL OR :status = '' OR :status = 'All' OR " +
           "(:status = 'Overdue' AND expectedCompletionDate > 0 AND expectedCompletionDate < :nowTime AND currentStatus NOT IN ('Released', 'Rejected', 'Cancelled')) OR " +
           "(:status = 'Completed' AND currentStatus IN ('Completed', 'Awaiting Collection', 'Released')) OR " +
           "(currentStatus = :status)) AND " +
           "(:query IS NULL OR :query = '' OR customerName LIKE '%' || :query || '%' OR deviceModel LIKE '%' || :query || '%' OR jobId LIKE '%' || :query || '%') " +
           "ORDER BY createdAt DESC")
    LiveData<List<RepairJob>> searchAndFilterJobs(String query, String status, long nowTime);

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus NOT IN ('Released', 'Rejected', 'Cancelled')")
    LiveData<Integer> getTotalJobsCount();

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus = :status")
    LiveData<Integer> getJobsCountByStatus(String status);

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus IN ('Received', 'Diagnosing')")
    LiveData<Integer> getAwaitingDiagnosisCount();

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus = 'Awaiting Approval'")
    LiveData<Integer> getAwaitingApprovalJobsCount();

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus = 'Repairing'")
    LiveData<Integer> getInRepairJobsCount();

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus = 'Testing' AND testingPassed = 0")
    LiveData<Integer> getFailedTestingCount();

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus IN ('Completed', 'Awaiting Collection', 'Released')")
    LiveData<Integer> getCompletedJobsCount();

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE currentStatus = 'Awaiting Collection' OR collectionStatus = 'AWAITING_COLLECTION'")
    LiveData<Integer> getAwaitingCollectionCount();

    @Query("SELECT COUNT(*) FROM repair_jobs WHERE expectedCompletionDate > 0 AND expectedCompletionDate < :nowTime AND currentStatus NOT IN ('Released', 'Rejected', 'Cancelled')")
    LiveData<Integer> getOverdueActiveJobsCount(long nowTime);
}
