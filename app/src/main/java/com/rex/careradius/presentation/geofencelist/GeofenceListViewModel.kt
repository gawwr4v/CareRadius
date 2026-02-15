package com.rex.careradius.presentation.geofencelist

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.rex.careradius.data.local.entity.GeofenceEntity
import com.rex.careradius.data.repository.GeofenceRepository
import com.rex.careradius.data.repository.VisitRepository
import com.rex.careradius.system.geofence.GeofenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

// tags: viewmodel, geofence, list, manage
class GeofenceListViewModel(
    // WARNING: Context passed here should be Application context to avoid leaks
    // In NavGraph currently passing LocalContext.current - acceptable for simple single-activity app
    private val context: Context,
    private val geofenceRepository: GeofenceRepository,
    private val visitRepository: VisitRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {
    
    // all geofences
    private val _geofences = MutableStateFlow<List<GeofenceEntity>>(emptyList())
    val geofences: StateFlow<List<GeofenceEntity>> = _geofences.asStateFlow()
    
    // location change state
    private val _changingLocationForGeofence = MutableStateFlow<GeofenceEntity?>(null)
    val changingLocationForGeofence: StateFlow<GeofenceEntity?> = _changingLocationForGeofence.asStateFlow()
    
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    init {
        viewModelScope.launch {
            geofenceRepository.getAllGeofences().collect {
                _geofences.value = it
            }
        }
    }
    
    // tags: delete, remove
    fun deleteGeofence(geofence: GeofenceEntity) {
        viewModelScope.launch {
            // unregister from android geofencing system
            geofenceManager.unregisterGeofence(geofence.id)
            
            // delete from database (cascades to visits)
            geofenceRepository.delete(geofence)
        }
    }
    
    // tags: update, edit, radius
    // tags: update, edit, radius
    fun updateGeofence(
        geofence: GeofenceEntity, 
        newName: String, 
        newRadius: Float, 
        newIcon: String,
        newEntryMessage: String,
        newExitMessage: String
    ) {
        viewModelScope.launch {
            // only close visit if radius shrinks and user is now outside
            if (newRadius < geofence.radius) {
                closeVisitIfOutside(geofence.latitude, geofence.longitude, newRadius, geofence.id)
            }
            
            val iconValue = newIcon.ifBlank { "📍" }
            val updated = geofence.copy(
                name = newName, 
                radius = newRadius, 
                icon = iconValue,
                entryMessage = newEntryMessage,
                exitMessage = newExitMessage
            )
            geofenceRepository.insert(updated) // REPLACE mode
            
            // re-register with updated radius
            geofenceManager.unregisterGeofence(updated.id)
            geofenceManager.registerGeofence(
                id = updated.id,
                latitude = updated.latitude,
                longitude = updated.longitude,
                radius = newRadius
            )
        }
    }
    
    // close visit only if user is actually outside the new boundary
    @SuppressLint("MissingPermission")
    private suspend fun closeVisitIfOutside(
        geofenceLat: Double,
        geofenceLng: Double,
        radius: Float,
        geofenceId: Long
    ) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val distance = calculateDistance(
                        location.latitude, location.longitude,
                        geofenceLat, geofenceLng
                    )
                    // only close if user is outside the new radius
                    if (distance > radius) {
                        viewModelScope.launch {
                            closeOpenVisitForGeofence(geofenceId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // if location unavailable, let the system geofence handle it
        }
    }
    
    // close any open visit for a geofence
    private suspend fun closeOpenVisitForGeofence(geofenceId: Long) {
        val openVisit = visitRepository.getOpenVisitForGeofence(geofenceId)
        if (openVisit != null) {
            val exitTime = System.currentTimeMillis()
            val duration = exitTime - openVisit.entryTime
            val closedVisit = openVisit.copy(
                exitTime = exitTime,
                durationMillis = duration
            )
            visitRepository.updateVisit(closedVisit)
        }
    }
    
    // Haversine formula
    // Precise enough for geofencing (meters)
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    // tags: update, location, move
    fun updateGeofenceLocation(
        geofence: GeofenceEntity,
        newLatitude: Double,
        newLongitude: Double
    ) {
        viewModelScope.launch {
            // close visit if user is outside new location
            closeVisitIfOutside(newLatitude, newLongitude, geofence.radius, geofence.id)
            
            val updated = geofence.copy(
                latitude = newLatitude,
                longitude = newLongitude
            )
            geofenceRepository.insert(updated) // REPLACE mode
            
            // re-register with new location
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

