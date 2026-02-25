package com.example.screentime.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screentime.data.ScreenTimeDatabase
import com.example.screentime.data.entities.AppLimit
import com.example.screentime.data.entities.DailyProgress
import com.example.screentime.data.entities.EarnedBadge
import com.example.screentime.data.repository.BadgeRepository
import com.example.screentime.data.repository.ScreenTimeRepository
import com.example.screentime.data.repository.WeeklyStatsRepository
import com.example.screentime.domain.managers.AppInfo
import com.example.screentime.domain.managers.AppLimitManager
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
    val installedApps: List<AppInfo> = emptyList(),
    val appLimits: Map<String, AppLimit> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    application: Application,
    private val screenTimeRepository: ScreenTimeRepository,
    private val badgeRepository: BadgeRepository,
    private val weeklyStatsRepository: WeeklyStatsRepository
) : AndroidViewModel(application) {

    private val database = ScreenTimeDatabase.getDatabase(application)
    private val appLimitManager = AppLimitManager(application, database.appLimitDao())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        loadInstalledApps()
        observeAppLimits()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DashboardViewModel", "Starting to load installed apps...")
                val apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    appLimitManager.getInstalledApps()
                }
                android.util.Log.d("DashboardViewModel", "Loaded ${apps.size} apps")
                _uiState.value = _uiState.value.copy(installedApps = apps)
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Failed to load apps", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(error = "Failed to load apps: ${e.message}")
            }
        }
    }

    private fun observeAppLimits() {
        viewModelScope.launch {
            appLimitManager.getAllLimits().collect { limits ->
                val limitsMap = limits.associateBy { it.packageName }
                _uiState.value = _uiState.value.copy(appLimits = limitsMap)
            }
        }
    }

    fun setAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            appLimitManager.setAppLimit(packageName, appName, limitMinutes)
        }
    }

    fun updateAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            appLimitManager.updateAppLimit(appLimit)
        }
    }

    fun deleteAppLimit(packageName: String) {
        viewModelScope.launch {
            val limit = _uiState.value.appLimits[packageName]
            if (limit != null) {
                appLimitManager.deleteAppLimit(limit)
            }
        }
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

                _uiState.value = _uiState.value.copy(
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

    fun refreshUsageNow() {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    appLimitManager.refreshUsageForAllLimits()
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error refreshing usage", e)
            }
        }
    }

    suspend fun getCurrentUsageMinutes(packageName: String): Int {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                appLimitManager.getTotalAppUsageMinutes(packageName)
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardViewModel", "Error getting current usage", e)
            0
        }
    }

    /**
     * Get past 7 days usage for a specific app
     * Returns a map of date (ISO format) -> total minutes used that day
     */
    suspend fun getPastUsageForApp(packageName: String): Map<String, Int> {
        return try {
            val usageMap = mutableMapOf<String, Int>()

            // Get all sessions from database
            val appUsageSessions = database.appUsageSessionDao()
                .getSessionsForPackage(packageName)
                .filter { session ->
                    try {
                        val sessionDate = java.time.LocalDate.parse(session.date)
                        val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(sessionDate, LocalDate.now())
                        daysAgo in 0..6 // Last 7 days including today
                    } catch (e: Exception) {
                        false
                    }
                }

            android.util.Log.d("DashboardViewModel", "Found ${appUsageSessions.size} sessions for $packageName")

            // Group by date and sum minutes
            appUsageSessions.groupBy { it.date }.forEach { (date, sessions) ->
                val totalMinutes = sessions.sumOf { it.durationMinutes }
                usageMap[date] = totalMinutes
                android.util.Log.d("DashboardViewModel", "  $date: $totalMinutes minutes (${sessions.size} sessions)")
            }

            android.util.Log.d("DashboardViewModel", "Total days with usage: ${usageMap.size}")
            usageMap
        } catch (e: Exception) {
            android.util.Log.e("DashboardViewModel", "Error getting past usage", e)
            emptyMap()
        }
    }
}
