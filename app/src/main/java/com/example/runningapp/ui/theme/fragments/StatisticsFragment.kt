package com.example.runningapp.ui.theme.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentStatisticsBinding
import com.example.runningapp.db.Run
import com.example.runningapp.db.RunningDatabase
import com.example.runningapp.other.CustomMarkerView
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.repositories.MainRepository
import com.example.runningapp.ui.theme.viewmodels.StatisticsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.round

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics){

    private val viewModel: StatisticsViewModel by viewModels()

    private lateinit var bindingStatistics: FragmentStatisticsBinding
    private lateinit var mainRepository: MainRepository

    private lateinit var totalTimeRun: LiveData<Long>
    private lateinit var totalDistance: LiveData<Int>
    private lateinit var totalAvgSpeed: LiveData<Float>
    private lateinit var runsSortedByDate: LiveData<List<Run>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val database = RunningDatabase.getInstance(requireContext())
        val runDAO = database.getRunDao()
        mainRepository = MainRepository(runDAO)
        bindingStatistics = FragmentStatisticsBinding.bind(view)

        totalTimeRun = mainRepository.getTotalTimeInMillis()
        totalDistance = mainRepository.getTotalDistance()
        totalAvgSpeed = mainRepository.getTotalAvgSpeed()
        runsSortedByDate = mainRepository.getAllRunsSortedByDate()

        subscribeToObservers()
        setupBarChart()
    }

    private fun setupBarChart() {
        bindingStatistics.barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawLabels(false)
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            setDrawGridLines(false)
        }
        bindingStatistics.barChart.axisLeft.apply {
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            setDrawGridLines(false)
        }
        bindingStatistics.barChart.axisRight.apply {
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            setDrawGridLines(false)
        }
        bindingStatistics.barChart.apply {
            description.text = "Average Speed Over Time"
            legend.isEnabled = false
        }
    }

    private fun subscribeToObservers() {
        totalTimeRun.observe(viewLifecycleOwner, Observer {
            it?.let {
                val obsTotalTimeRun = TrackingUtility.getFormattedStopWatchTime(it)
                bindingStatistics.tvTotalTime.text = obsTotalTimeRun
            }
        })
        totalDistance.observe(viewLifecycleOwner, Observer {
            it?.let {
                val km = it / 1000f
                val obsTotalDistance = round(km * 10f) / 10f
                val totalDistanceString = "${obsTotalDistance}km"
                bindingStatistics.tvTotalDistance.text = totalDistanceString
            }
        })
        totalAvgSpeed.observe(viewLifecycleOwner, Observer {
            it?.let {
                val obsAvgSpeed = round(it * 10f) / 10f
                val avgSpeedString = "${obsAvgSpeed}km/h"
                bindingStatistics.tvAverageSpeed.text = avgSpeedString
            }
        })
        runsSortedByDate.observe(viewLifecycleOwner, Observer {
            it?.let {
                val totalRunsString = it.size.toString()
                bindingStatistics.tvTotalRuns.text = totalRunsString
                val allAvgSpeeds = it.indices.map { i ->
                    BarEntry(i.toFloat(), it[i].avgSpeedInKMH)
                }
                val barDataSet = BarDataSet(allAvgSpeeds, "Average Speed Over Time")
                    .apply {
                        valueTextColor = Color.BLACK
                        color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
                    }
                bindingStatistics.barChart.data = BarData(barDataSet)
                bindingStatistics.barChart.marker = CustomMarkerView(it.reversed(), requireContext(), R.layout.marker_view)
                bindingStatistics.barChart.invalidate()
            }
        })
    }
}