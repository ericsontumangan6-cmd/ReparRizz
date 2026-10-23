package com.example.fixflow.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.fixflow.data.entity.Payment;

import java.util.List;

@Dao
public interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPayment(Payment payment);

    @Query("SELECT * FROM payments WHERE jobId = :jobId ORDER BY paymentTimestamp ASC")
    LiveData<List<Payment>> getPaymentsForJob(String jobId);

    @Query("SELECT * FROM payments WHERE jobId = :jobId ORDER BY paymentTimestamp ASC")
    List<Payment> getPaymentsForJobSync(String jobId);

    @Query("SELECT SUM(repairAmount) FROM payments WHERE jobId = :jobId")
    LiveData<Double> getTotalRepairPaidForJob(String jobId);

    @Query("SELECT SUM(repairAmount) FROM payments WHERE jobId = :jobId")
    Double getTotalRepairPaidForJobSync(String jobId);

    @Query("SELECT SUM(tipAmount) FROM payments WHERE jobId = :jobId")
    LiveData<Double> getTotalTipsForJob(String jobId);

    @Query("SELECT SUM(tipAmount) FROM payments WHERE jobId = :jobId")
    Double getTotalTipsForJobSync(String jobId);

    @Query("SELECT SUM(amount) FROM payments WHERE jobId = :jobId")
    LiveData<Double> getTotalCashReceivedForJob(String jobId);

    @Query("SELECT SUM(amount) FROM payments WHERE jobId = :jobId")
    Double getTotalCashReceivedForJobSync(String jobId);

    @Query("SELECT SUM(repairAmount) FROM payments")
    LiveData<Double> getTotalRepairRevenueAll();

    @Query("SELECT SUM(repairAmount) FROM payments")
    Double getTotalRepairRevenueAllSync();

    @Query("SELECT SUM(tipAmount) FROM payments")
    LiveData<Double> getTotalTipsAll();

    @Query("SELECT SUM(tipAmount) FROM payments")
    Double getTotalTipsAllSync();

    @Query("SELECT SUM(amount) FROM payments")
    LiveData<Double> getTotalCashAll();

    @Query("SELECT SUM(amount) FROM payments")
    Double getTotalCashAllSync();
}
