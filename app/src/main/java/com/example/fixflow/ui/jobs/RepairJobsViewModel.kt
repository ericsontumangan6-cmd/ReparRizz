package com.example.fixflow.ui.jobs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.fixflow.data.db.FixFlowDatabase
import com.example.fixflow.data.entity.RepairJob
import com.example.fixflow.data.repository.RepairRepository

class RepairJobsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RepairRepository

    private val searchQuery = MutableLiveData<String?>("")
    private val selectedStatusFilter = MutableLiveData<String?>("All")

    val jobsList: LiveData<List<RepairJob>>

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

        val combinedFilter = MutableLiveData<Pair<String?, String?>>()
        
        fun updateCombined() {
            combinedFilter.value = Pair(searchQuery.value, selectedStatusFilter.value)
        }

        searchQuery.observeForever { updateCombined() }
        selectedStatusFilter.observeForever { updateCombined() }

        jobsList = combinedFilter.switchMap { (query, status) ->
            repository.searchAndFilterJobs(query, status)
        }
    }

    fun setSearchQuery(query: String?) {
        searchQuery.value = query
    }

    fun setStatusFilter(status: String?) {
        selectedStatusFilter.value = status
    }
}
