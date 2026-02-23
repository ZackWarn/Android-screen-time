package com.example.screentime.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.screentime.data.entities.DailyProgress
import com.example.screentime.data.entities.EarnedBadge
import com.example.screentime.data.repository.BadgeRepository
import com.example.screentime.data.repository.ScreenTimeRepository
import com.example.screentime.data.repository.WeeklyStatsRepository
import com.example.screentime.utils.WeekUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val weekNumber: Int = 0,
    val currentWeekPoints: Int = 0,
    val weeklyProgress: List<DailyProgress> = emptyList(),
    val earnedBadges: List<EarnedBadge> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    private val screenTimeRepository: ScreenTimeRepository,
    private val badgeRepository: BadgeRepository,
    private val weeklyStatsRepository: WeeklyStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val weekNumber = WeekUtils.getCurrentWeekNumber()
                val weekStart = WeekUtils.getWeekStartDate(weekNumber)
                val weekEnd = WeekUtils.getWeekEndDate(weekNumber)

                val weeklyProgress = screenTimeRepository.getWeeklyProgress(weekStart, weekEnd)
                val totalPoints = weeklyProgress.sumOf { it.pointsEarned }
                val earnedBadges = badgeRepository.getBadgesForWeek(weekNumber)

                _uiState.value = DashboardUiState(
                    weekNumber = weekNumber,
                    currentWeekPoints = totalPoints,
                    weeklyProgress = weeklyProgress,
                    earnedBadges = earnedBadges,
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
        loadDashboardData()
    }
}

