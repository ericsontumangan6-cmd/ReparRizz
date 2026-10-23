package com.example.fixflow.ui.jobs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fixflow.R
import com.example.fixflow.databinding.FragmentRepairJobListBinding

class RepairJobListFragment : Fragment() {

    private var _binding: FragmentRepairJobListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RepairJobsViewModel by viewModels()
    private lateinit var adapter: RepairJobAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepairJobListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchAndFilter()
        observeViewModel()

        val initialFilter = arguments?.getString("filterStatus")
        if (!initialFilter.isNullOrEmpty()) {
            selectChipForFilter(initialFilter)
        }
    }

    private fun setupRecyclerView() {
        adapter = RepairJobAdapter { jobId ->
            val bundle = Bundle().apply {
                putString("jobId", jobId)
            }
            findNavController().navigate(R.id.nav_repair_detail, bundle)
        }
        binding.rvJobsList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobsList.adapter = adapter
    }

    private fun setupSearchAndFilter() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chip_received -> "Received"
                R.id.chip_diagnosing -> "Diagnosing"
                R.id.chip_awaiting_approval -> "Awaiting Approval"
                R.id.chip_repairing -> "Repairing"
                R.id.chip_testing -> "Testing"
                R.id.chip_completed -> "Completed"
                R.id.chip_awaiting_collection -> "Awaiting Collection"
                R.id.chip_overdue -> "Overdue"
                R.id.chip_rejected -> "Rejected"
                else -> "All"
            }
            viewModel.setStatusFilter(status)
        }
    }

    private fun selectChipForFilter(filter: String) {
        val chipId = when (filter) {
            "Received" -> R.id.chip_received
            "Diagnosing" -> R.id.chip_diagnosing
            "Awaiting Approval" -> R.id.chip_awaiting_approval
            "Repairing" -> R.id.chip_repairing
            "Testing" -> R.id.chip_testing
            "Completed" -> R.id.chip_completed
            "Awaiting Collection" -> R.id.chip_awaiting_collection
            "Overdue" -> R.id.chip_overdue
            "Rejected" -> R.id.chip_rejected
            else -> R.id.chip_all
        }
        binding.chipGroupStatus.check(chipId)
    }

    private fun observeViewModel() {
        viewModel.jobsList.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                binding.tvEmptyJobs.visibility = View.VISIBLE
                binding.rvJobsList.visibility = View.GONE
            } else {
                binding.tvEmptyJobs.visibility = View.GONE
                binding.rvJobsList.visibility = View.VISIBLE
                adapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
