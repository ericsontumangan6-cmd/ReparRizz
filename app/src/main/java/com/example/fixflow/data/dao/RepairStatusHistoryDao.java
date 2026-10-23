package com.example.fixflow.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fixflow.data.entity.RepairStatusHistory;

import java.util.List;

@Dao
public interface RepairStatusHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHistory(RepairStatusHistory history);

    @Query("SELECT * FROM repair_status_history WHERE jobId = :jobId ORDER BY timestamp ASC")
    LiveData<List<RepairStatusHistory>> getHistoryForJob(String jobId);

    @Query("SELECT * FROM repair_status_history WHERE jobId = :jobId ORDER BY timestamp ASC")
    List<RepairStatusHistory> getHistoryForJobSync(String jobId);

    @Query("SELECT * FROM repair_status_history ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<RepairStatusHistory>> getRecentHistory(int limit);

    @Query("SELECT * FROM repair_status_history ORDER BY timestamp ASC")
    List<RepairStatusHistory> getAllHistorySync();
}
