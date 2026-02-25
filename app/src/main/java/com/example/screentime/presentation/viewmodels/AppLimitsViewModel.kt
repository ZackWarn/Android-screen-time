package com.example.screentime.presentation.viewmodels
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screentime.data.ScreenTimeDatabase
import com.example.screentime.data.entities.AppLimit
import com.example.screentime.domain.managers.AppInfo
import com.example.screentime.domain.managers.AppLimitManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
class AppLimitsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ScreenTimeDatabase.getDatabase(application)
    private val appLimitManager = AppLimitManager(application, database.appLimitDao())
    private val _appLimits = MutableStateFlow<List<AppLimit>>(emptyList())
    val appLimits = _appLimits.asStateFlow()
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()
    init {
        loadAppLimits()
        loadInstalledApps()
    }
    private fun loadAppLimits() {
        viewModelScope.launch {
            appLimitManager.getAllLimits().collect { limits ->
                _appLimits.value = limits
            }
        }
    }
    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = appLimitManager.getInstalledApps()
        }
    }
    fun addAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            appLimitManager.setAppLimit(packageName, appName, limitMinutes)
        }
    }
    fun updateAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            appLimitManager.updateAppLimit(appLimit)
        }
    }
    fun deleteAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            appLimitManager.deleteAppLimit(appLimit)
        }
    }
    fun toggleAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            val updated = appLimit.copy(isEnabled = !appLimit.isEnabled)
            appLimitManager.updateAppLimit(updated)
        }
    }
}
