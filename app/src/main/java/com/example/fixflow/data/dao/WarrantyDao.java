package com.example.fixflow.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fixflow.data.entity.Warranty;

@Dao
public interface WarrantyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWarranty(Warranty warranty);

    @Query("SELECT * FROM warranties WHERE jobId = :jobId LIMIT 1")
    LiveData<Warranty> getWarrantyForJob(String jobId);

    @Query("SELECT * FROM warranties WHERE jobId = :jobId LIMIT 1")
    Warranty getWarrantyForJobSync(String jobId);
}
