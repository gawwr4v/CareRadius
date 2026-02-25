package com.rex.careradius.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rex.careradius.data.local.dao.GeofenceDao
import com.rex.careradius.data.local.dao.VisitDao
import com.rex.careradius.data.repository.UserPreferencesRepository
import com.rex.careradius.system.geofence.GeofenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geofenceDao: GeofenceDao,
    private val visitDao: VisitDao,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = userPreferencesRepository.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false 
        )

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferencesRepository.isNotificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val activeGeofencesCount: StateFlow<Int> = geofenceDao.getAllGeofences()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkTheme(isDark)
        }
    }

    fun toggleNotifications(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(isEnabled)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            // Unregister all active geofences from OS first
            val geofences = geofenceDao.getAllGeofences().first()
            geofences.forEach { geofenceManager.unregisterGeofence(it.id) }
            
            // Delete all records in DB (Geofences cascade to active visits, but we wipe visits explicitly to be safe)
            visitDao.deleteAllVisits() 
            // geofenceDao doesn't have a deleteAll yet; we can delete one by one since it's a small list
            geofences.forEach { geofenceDao.delete(it) }
        }
    }
}

class SettingsViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geofenceDao: GeofenceDao,
    private val visitDao: VisitDao,
    private val geofenceManager: GeofenceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                userPreferencesRepository,
                geofenceDao,
                visitDao,
                geofenceManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
