package com.rex.careradius.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rex.careradius.data.local.entity.GeofenceEntity
import com.rex.careradius.data.repository.GeofenceRepository
import com.rex.careradius.system.geofence.GeofenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Simple data class to represent a location
 */
data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double
)

/**
 * ViewModel for Map Screen
 * Manages geofence creation, editing, and display
 */
class MapViewModel(
    private val geofenceRepository: GeofenceRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {
    
    // State for all geofences
    private val _geofences = MutableStateFlow<List<GeofenceEntity>>(emptyList())
    val geofences: StateFlow<List<GeofenceEntity>> = _geofences.asStateFlow()
    
    // Dialog state
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()
    
    private val _showAddPinDialog = MutableStateFlow(false)
    val showAddPinDialog: StateFlow<Boolean> = _showAddPinDialog.asStateFlow()
    
    private val _showCoordinateDialog = MutableStateFlow(false)
    val showCoordinateDialog: StateFlow<Boolean> = _showCoordinateDialog.asStateFlow()
    
    private val _dropPinMode = MutableStateFlow(false)
    val dropPinMode: StateFlow<Boolean> = _dropPinMode.asStateFlow()
    
    // Geofence ID being edited for location change
    private val _changingLocationForGeofenceId = MutableStateFlow<Long?>(null)
    val changingLocationForGeofenceId: StateFlow<Long?> = _changingLocationForGeofenceId.asStateFlow()
    
    private val _selectedLocation = MutableStateFlow<LocationCoordinates?>(null)
    val selectedLocation: StateFlow<LocationCoordinates?> = _selectedLocation.asStateFlow()
    
    private val _editingGeofence = MutableStateFlow<GeofenceEntity?>(null)
    val editingGeofence: StateFlow<GeofenceEntity?> = _editingGeofence.asStateFlow()
    
    // Form state
    private val _geofenceName = MutableStateFlow("")
    val geofenceName: StateFlow<String> = _geofenceName.asStateFlow()
    
    private val _radius = MutableStateFlow(30f) // Default 30m
    val radius: StateFlow<Float> = _radius.asStateFlow()
    
    // Manual coordinate entry
    private val _manualLat = MutableStateFlow("")
    val manualLat: StateFlow<String> = _manualLat.asStateFlow()
    
    private val _manualLng = MutableStateFlow("")
    val manualLng: StateFlow<String> = _manualLng.asStateFlow()
    
    private val _icon = MutableStateFlow("📍")
    val icon: StateFlow<String> = _icon.asStateFlow()

    private val _entryMessage = MutableStateFlow("")
    val entryMessage: StateFlow<String> = _entryMessage.asStateFlow()

    private val _exitMessage = MutableStateFlow("")
    val exitMessage: StateFlow<String> = _exitMessage.asStateFlow()
    
    // Validation
    val isAddButtonEnabled: StateFlow<Boolean> = combine(
        _geofenceName,
        _radius
    ) { name, radius ->
        name.isNotBlank() && radius in 10f..50f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    
    init {
        loadGeofences()
    }
    
    private fun loadGeofences() {
        viewModelScope.launch {
            geofenceRepository.getAllGeofences().collect { geofenceList ->
                _geofences.value = geofenceList
            }
        }
    }
    
    // Long-press on map
    fun onMapLongPress(location: LocationCoordinates) {
        _selectedLocation.value = location
        _showDialog.value = true
        _editingGeofence.value = null
        // Reset form
        _geofenceName.value = ""
        _radius.value = 30f
        _icon.value = "📍"
        _entryMessage.value = ""
        _exitMessage.value = ""
    }
    
    // Add Pin button clicked
    fun onAddPinClicked() {
        _showAddPinDialog.value = true
    }
    
    fun onAddPinDialogDismiss() {
        _showAddPinDialog.value = false
    }
    
    // Drop pin at current location
    fun onDropPinHere(location: LocationCoordinates) {
        _showAddPinDialog.value = false
        _dropPinMode.value = true
    }
    
    fun onCancelDropPin() {
        _dropPinMode.value = false
        _changingLocationForGeofenceId.value = null  // Clear location change mode
    }
    
    fun onConfirmDropPin(location: LocationCoordinates) {
        _selectedLocation.value = location
        _dropPinMode.value = false
        
        // Check if we're changing location for an existing geofence
        val geofenceId = _changingLocationForGeofenceId.value
        if (geofenceId != null) {
            // Update existing geofence location
            viewModelScope.launch {
                val geofence = geofenceRepository.getGeofenceById(geofenceId)
                geofence?.let {
                    val updated = it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    geofenceRepository.insert(updated)
                    
                    // Re-register with new location
                    geofenceManager.unregisterGeofence(updated.id)
                    geofenceManager.registerGeofence(
                        id = updated.id,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radius = updated.radius
                    )
                    
                    _changingLocationForGeofenceId.value = null
                }
            }
        } else {
            // New geofence - show creation dialog
            _showDialog.value = true
            _editingGeofence.value = null
            _editingGeofence.value = null
            _geofenceName.value = ""
            _radius.value = 30f
            _icon.value = "📍"
            _entryMessage.value = ""
            _exitMessage.value = ""
        }
    }
    
    // Start location change mode for existing geofence
    fun startLocationChangeMode(geofenceId: Long) {
        _changingLocationForGeofenceId.value = geofenceId
        _dropPinMode.value = true
    }
    
    // Enter coordinates manually
    fun onEnterCoordinates() {
        _showAddPinDialog.value = false
        _showCoordinateDialog.value = true
        _manualLat.value = ""
        _manualLng.value = ""
    }
    
    fun onManualLatChange(lat: String) {
        _manualLat.value = lat
    }
    
    fun onManualLngChange(lng: String) {
        _manualLng.value = lng
    }
    
    fun onIconChange(newIcon: String) {
        _icon.value = newIcon
    }
    
    fun onCoordinateDialogDismiss() {
        _showCoordinateDialog.value = false
    }
    
    fun onConfirmCoordinates() {
        val lat = _manualLat.value.toDoubleOrNull()
        val lng = _manualLng.value.toDoubleOrNull()
        
        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
            _selectedLocation.value = LocationCoordinates(lat, lng)
            _showCoordinateDialog.value = false
            _showDialog.value = true
            _editingGeofence.value = null
            _editingGeofence.value = null
            _geofenceName.value = ""
            _radius.value = 30f
            _icon.value = "📍"
            _entryMessage.value = ""
            _exitMessage.value = ""
        }
    }
    
    // Tap on existing marker to edit
    fun onMarkerTapped(geofenceId: Long) {
        viewModelScope.launch {
            val geofence = geofenceRepository.getGeofenceById(geofenceId)
            geofence?.let {
                _editingGeofence.value = it
                _selectedLocation.value = LocationCoordinates(it.latitude, it.longitude)
                _geofenceName.value = it.name
                _radius.value = it.radius
                _icon.value = it.icon
                _entryMessage.value = it.entryMessage
                _exitMessage.value = it.exitMessage
                _showDialog.value = true
            }
        }
    }
    
    // Long-press on marker to edit (same as tap)
    fun onMarkerLongPressed(geofenceId: Long) {
        onMarkerTapped(geofenceId)
    }
    
    fun onNameChange(name: String) {
        _geofenceName.value = name
    }
    
    fun onRadiusChange(radius: Float) {
        _radius.value = radius.coerceIn(10f, 50f)
    }

    fun onEntryMessageChange(message: String) {
        _entryMessage.value = message
    }

    fun onExitMessageChange(message: String) {
        _exitMessage.value = message
    }
    
    fun onDialogDismiss() {
        _showDialog.value = false
        _selectedLocation.value = null
        _editingGeofence.value = null
    }
    
    fun onAddGeofence() {
        val location = _selectedLocation.value ?: return
        val name = _geofenceName.value
        val radiusValue = _radius.value
        val iconValue = _icon.value.ifBlank { "📍" }  // Default to pin if blank
        val entryMsg = _entryMessage.value
        val exitMsg = _exitMessage.value
        
        if (name.isBlank() || radiusValue !in 10f..50f) {
            return
        }
        
        viewModelScope.launch {
            val editingGeofenceValue = _editingGeofence.value
            
            if (editingGeofenceValue != null) {
                // Update existing geofence
                val updated = editingGeofenceValue.copy(
                    name = name,
                    radius = radiusValue,
                    icon = iconValue,
                    entryMessage = entryMsg,
                    exitMessage = exitMsg
                )
                geofenceRepository.insert(updated) // REPLACE mode
                
                // Re-register with updated parameters
                geofenceManager.unregisterGeofence(updated.id)
                geofenceManager.registerGeofence(
                    id = updated.id,
                    latitude = updated.latitude,
                    longitude = updated.longitude,
                    radius = radiusValue
                )
            } else {
                // Create new geofence
                val geofence = GeofenceEntity(
                    name = name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = radiusValue,
                    createdAt = System.currentTimeMillis(),
                    icon = iconValue,
                    entryMessage = entryMsg,
                    exitMessage = exitMsg
                )
                
                val geofenceId = geofenceRepository.insert(geofence)
                
                // Register with system
                geofenceManager.registerGeofence(
                    id = geofenceId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = radiusValue,
                    onSuccess = {},
                    onError = { }
                )
            }
            
            // Close dialog
            onDialogDismiss()
        }
    }
}
