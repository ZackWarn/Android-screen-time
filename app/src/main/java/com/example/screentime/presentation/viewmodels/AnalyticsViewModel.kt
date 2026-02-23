package com.example.screentime.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.screentime.data.entities.DailyProgress
import com.example.screentime.data.entities.WeeklyStats
import com.example.screentime.data.repository.ScreenTimeRepository
import com.example.screentime.data.repository.WeeklyStatsRepository
import com.example.screentime.utils.WeekUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AnalyticsUiState(
    val currentWeekStats: WeeklyStats? = null,
    val previousWeekStats: WeeklyStats? = null,
    val currentWeekProgress: List<DailyProgress> = emptyList(),
    val previousWeekProgress: List<DailyProgress> = emptyList(),
    val improvementPercentage: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AnalyticsViewModel(
    private val screenTimeRepository: ScreenTimeRepository,
    private val weeklyStatsRepository: WeeklyStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalyticsData()
    }

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentWeekNumber = WeekUtils.getCurrentWeekNumber()
                val currentYear = WeekUtils.getCurrentYear()
                val prevWeekNumber = WeekUtils.getPreviousWeekNumber()
                val prevWeekYear = WeekUtils.getPreviousWeekYear()

                // Get week dates
                val currentWeekStart = WeekUtils.getWeekStartDate(currentWeekNumber)
                val currentWeekEnd = WeekUtils.getWeekEndDate(currentWeekNumber)
                val prevWeekStart = WeekUtils.getWeekStartDate(prevWeekNumber, prevWeekYear)
                val prevWeekEnd = WeekUtils.getWeekEndDate(prevWeekNumber, prevWeekYear)

                // Fetch data
                val currentWeekStats = weeklyStatsRepository.getOrCreateStatsForWeek(currentWeekNumber, currentYear)
                val previousWeekStats = weeklyStatsRepository.getStatsForWeek(prevWeekNumber, prevWeekYear)
                val currentWeekProgress = screenTimeRepository.getWeeklyProgress(currentWeekStart, currentWeekEnd)
                val previousWeekProgress = screenTimeRepository.getWeeklyProgress(prevWeekStart, prevWeekEnd)

                // Calculate improvement
                val currentTotal = currentWeekProgress.sumOf { it.screenTimeMinutes }
                val prevTotal = previousWeekProgress.sumOf { it.screenTimeMinutes }
                val improvement = if (prevTotal > 0) {
                    ((prevTotal - currentTotal).toFloat() / prevTotal) * 100
                } else {
                    0f
                }

                _uiState.value = AnalyticsUiState(
                    currentWeekStats = currentWeekStats,
                    previousWeekStats = previousWeekStats,
                    currentWeekProgress = currentWeekProgress,
                    previousWeekProgress = previousWeekProgress,
                    improvementPercentage = improvement,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun refreshData() {
        loadAnalyticsData()
    }
}

