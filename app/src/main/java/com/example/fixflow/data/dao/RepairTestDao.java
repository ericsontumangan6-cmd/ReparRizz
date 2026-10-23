package com.example.fixflow.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fixflow.data.entity.RepairTest;

import java.util.List;

@Dao
public interface RepairTestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTest(RepairTest test);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllTests(List<RepairTest> tests);

    @Query("SELECT * FROM repair_tests WHERE jobId = :jobId ORDER BY testId ASC")
    LiveData<List<RepairTest>> getTestsForJob(String jobId);

    @Query("SELECT * FROM repair_tests WHERE jobId = :jobId ORDER BY testId ASC")
    List<RepairTest> getTestsForJobSync(String jobId);

    @Query("DELETE FROM repair_tests WHERE jobId = :jobId")
    void deleteTestsForJob(String jobId);
}
