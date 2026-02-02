package com.rex.careradius.presentation.geofencelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rex.careradius.data.local.entity.GeofenceEntity
import com.rex.careradius.data.repository.GeofenceRepository
import com.rex.careradius.system.geofence.GeofenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Geofence List Screen
 * Manages geofence display and deletion
 */
class GeofenceListViewModel(
    private val geofenceRepository: GeofenceRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {
    
    // All geofences
    private val _geofences = MutableStateFlow<List<GeofenceEntity>>(emptyList())
    val geofences: StateFlow<List<GeofenceEntity>> = _geofences.asStateFlow()
    
    // Location change state
    private val _changingLocationForGeofence = MutableStateFlow<GeofenceEntity?>(null)
    val changingLocationForGeofence: StateFlow<GeofenceEntity?> = _changingLocationForGeofence.asStateFlow()
    
    init {
        viewModelScope.launch {
            geofenceRepository.getAllGeofences().collect {
                _geofences.value = it
            }
        }
    }
    
    /**
     * Delete a geofence from database and unregister from system
     */
    fun deleteGeofence(geofence: GeofenceEntity) {
        viewModelScope.launch {
            // Unregister from Android geofencing system
            geofenceManager.unregisterGeofence(geofence.id)
            
            // Delete from database (cascades to visits)
            geofenceRepository.delete(geofence)
        }
    }
    
    /**
     * Update a geofence (name and/or radius and/or icon)
     */
    fun updateGeofence(geofence: GeofenceEntity, newName: String, newRadius: Float, newIcon: String) {
        viewModelScope.launch {
            val iconValue = newIcon.ifBlank { "📍" }  // Default to pin if blank
            val updated = geofence.copy(name = newName, radius = newRadius, icon = iconValue)
            geofenceRepository.insert(updated) // REPLACE mode
            
            // Re-register with updated radius
            geofenceManager.unregisterGeofence(updated.id)
            geofenceManager.registerGeofence(
                id = updated.id,
                latitude = updated.latitude,
                longitude = updated.longitude,
                radius = newRadius
            )
        }
    }
    
    /**
     * Update geofence location
     */
    fun updateGeofenceLocation(
        geofence: GeofenceEntity,
        newLatitude: Double,
        newLongitude: Double
    ) {
        viewModelScope.launch {
            val updated = geofence.copy(
                latitude = newLatitude,
                longitude = newLongitude
            )
            geofenceRepository.insert(updated) // REPLACE mode
            
            // Re-register with new location
            geofenceManager.unregisterGeofence(updated.id)
            geofenceManager.registerGeofence(
                id = updated.id,
                latitude = newLatitude,
                longitude = newLongitude,
                radius = updated.radius
            )
            
            _changingLocationForGeofence.value = null
        }
    }
    
    fun startChangingLocation(geofence: GeofenceEntity) {
        _changingLocationForGeofence.value = geofence
    }
    
    fun cancelChangingLocation() {
        _changingLocationForGeofence.value = null
    }
}
