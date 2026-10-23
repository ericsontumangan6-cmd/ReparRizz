package com.example.fixflow.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fixflow.data.entity.WarrantyClaim;

import java.util.List;

@Dao
public interface WarrantyClaimDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertClaim(WarrantyClaim claim);

    @Update
    void updateClaim(WarrantyClaim claim);

    @Query("SELECT * FROM warranty_claims WHERE originalJobId = :jobId ORDER BY timestamp DESC")
    LiveData<List<WarrantyClaim>> getClaimsForJob(String jobId);

    @Query("SELECT * FROM warranty_claims ORDER BY timestamp DESC")
    LiveData<List<WarrantyClaim>> getAllClaims();

    @Query("SELECT * FROM warranty_claims ORDER BY timestamp DESC")
    List<WarrantyClaim> getAllClaimsSync();
}
