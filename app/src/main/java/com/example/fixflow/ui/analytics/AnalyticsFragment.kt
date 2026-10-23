package com.example.fixflow.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fixflow.databinding.FragmentAnalyticsBinding
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAnalytics()
    }

    private fun observeViewModel() {
        viewModel.analyticsData.observe(viewLifecycleOwner) { summary ->
            if (summary == null) return@observe

            binding.tvAnalyticsPayments.text = String.format(Locale.US, "₱%.2f", summary.totalRepairRevenue)
            binding.tvAnalyticsTips.text = String.format(Locale.US, "₱%.2f", summary.totalCustomerTips)
            binding.tvAnalyticsBalances.text = String.format(Locale.US, "₱%.2f", summary.totalOutstandingBalances)

            binding.tvAnalyticsTotalJobs.text = summary.totalJobs.toString()
            binding.tvAnalyticsCompletedJobs.text = summary.completedJobs.toString()
            binding.tvAnalyticsTurnaround.text = String.format(Locale.US, "%.1f hrs", summary.avgTurnaroundTimeHours)
            binding.tvAnalyticsWarrantyClaims.text = summary.totalWarrantyClaims.toString()

            // Populate top problem types list dynamically
            binding.layoutProblemsContainer.removeAllViews()
            if (summary.topProblemTypes.isEmpty()) {
                binding.tvNoProblemsData.visibility = View.VISIBLE
            } else {
                binding.tvNoProblemsData.visibility = View.GONE

                val maxCount = summary.topProblemTypes.maxOfOrNull { it.second } ?: 1

                for ((problem, count) in summary.topProblemTypes) {
                    val rowView = createProblemRow(problem, count, maxCount)
                    binding.layoutProblemsContainer.addView(rowView)
                }
            }
        }
    }

    private fun createProblemRow(problem: String, count: Int, maxCount: Int): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val textRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val tvProblem = TextView(requireContext()).apply {
            text = problem
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvCount = TextView(requireContext()).apply {
            text = "$count jobs"
            setTextColor(Color.parseColor("#35D6C3"))
            textSize = 12f
        }

        textRow.addView(tvProblem)
        textRow.addView(tvCount)

        val progressBarBg = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8
            ).apply {
                topMargin = 6
            }
            setBackgroundColor(Color.parseColor("#151C28"))
        }

        val barWidthRatio = count.toFloat() / maxCount
        val progressBarFill = View(requireContext()).apply {
            val fillParams = LinearLayout.LayoutParams(
                0,
                8
            ).apply {
                topMargin = -8
            }
            fillParams.weight = barWidthRatio
            layoutParams = fillParams
            setBackgroundColor(Color.parseColor("#35D6C3"))
        }

        container.addView(textRow)
        container.addView(progressBarBg)

        return container
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
