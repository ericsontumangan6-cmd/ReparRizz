package com.example.fixflow.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fixflow.data.entity.RepairEstimate;

import java.util.List;

@Dao
public interface RepairEstimateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEstimate(RepairEstimate estimate);

    @Query("SELECT * FROM repair_estimates WHERE jobId = :jobId ORDER BY estimateVersion DESC")
    LiveData<List<RepairEstimate>> getEstimatesForJob(String jobId);

    @Query("SELECT * FROM repair_estimates WHERE jobId = :jobId ORDER BY estimateVersion DESC LIMIT 1")
    RepairEstimate getLatestEstimateSync(String jobId);

    @Query("SELECT * FROM repair_estimates WHERE jobId = :jobId ORDER BY estimateVersion DESC LIMIT 1")
    LiveData<RepairEstimate> getLatestEstimate(String jobId);
}
