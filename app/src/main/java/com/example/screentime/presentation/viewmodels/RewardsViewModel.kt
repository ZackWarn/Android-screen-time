package com.example.screentime.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.screentime.data.entities.EarnedBadge
import com.example.screentime.data.repository.BadgeRepository
import com.example.screentime.utils.WeekUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RewardsUiState(
    val currentWeekPoints: Int = 0,
    val earnedBadges: List<EarnedBadge> = emptyList(),
    val badgeCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RewardsViewModel(
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        loadRewardsData()
    }

    fun loadRewardsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val weekNumber = WeekUtils.getCurrentWeekNumber()
                val earnedBadges = badgeRepository.getBadgesForWeek(weekNumber)
                val badgeCount = badgeRepository.getBadgeCountForWeek(weekNumber)

                _uiState.value = RewardsUiState(
                    earnedBadges = earnedBadges,
                    badgeCount = badgeCount,
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
        loadRewardsData()
    }
}

