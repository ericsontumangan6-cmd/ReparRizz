package com.example.fixflow.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fixflow.data.db.FixFlowDatabase
import com.example.fixflow.data.repository.RepairRepository
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RepairRepository

    private val _analyticsData = MutableLiveData<RepairRepository.AnalyticsSummary>()
    val analyticsData: LiveData<RepairRepository.AnalyticsSummary> = _analyticsData

    init {
        val db = FixFlowDatabase.getDatabase(application)
        repository = RepairRepository(
            db.repairJobDao(),
            db.repairEstimateDao(),
             db.repairStatusHistoryDao(),
            db.repairTestDao(),
            db.paymentDao(),
            db.warrantyDao(),
            db.warrantyClaimDao()
        )
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _analyticsData.value = repository.calculateAnalytics()
        }
    }
}
