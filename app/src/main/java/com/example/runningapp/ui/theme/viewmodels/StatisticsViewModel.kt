package com.example.runningapp.ui.theme.viewmodels

import androidx.lifecycle.ViewModel
import com.example.runningapp.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {

    val totalTimeRun = mainRepository.getTotalTimeInMillis()
    val totalDistance = mainRepository.getTotalDistance()
    val totalAvgSpeed = mainRepository.getTotalAvgSpeed()

    val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
}