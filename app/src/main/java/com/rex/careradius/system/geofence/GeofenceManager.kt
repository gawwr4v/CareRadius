package com.rex.careradius.system.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.data.repository.GeofenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// tags: geofence, location, gps, register
class GeofenceManager(private val context: Context) {
    
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    
    // pending intent for receiving geofence transitions
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
    
    // tags: register, add, create
    fun registerGeofence(
        id: Long,
        latitude: Double,
        longitude: Double,
        radius: Float,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        android.util.Log.d("GeofenceManager", "registerGeofence called: id=$id, lat=$latitude, lng=$longitude, radius=$radius")
        
        if (!hasLocationPermissions()) {
            android.util.Log.e("GeofenceManager", "Location permissions not granted")
            onError("Location permissions not granted")
            return
        }
        
        val geofence = Geofence.Builder()
            .setRequestId(id.toString())
            .setCircularRegion(latitude, longitude, radius)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
        
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        
        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener { 
                    android.util.Log.d("GeofenceManager", "Geofence registered successfully: $id")
                    onSuccess() 
                }
                addOnFailureListener { exception ->
                    android.util.Log.e("GeofenceManager", "Failed to add geofence: ${exception.message}")
                    onError("Failed to add geofence: ${exception.message}")
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.e("GeofenceManager", "Security exception: ${e.message}")
            onError("Security exception: ${e.message}")
        }
    }
    
    // tags: remove, delete, unregister
    fun unregisterGeofence(id: Long) {
        try {
            geofencingClient.removeGeofences(listOf(id.toString()))
        } catch (e: SecurityException) {
            // silently fail, geofence might already be removed
        }
    }
    
    // called on app startup to restore geofences after device restart
    // tags: restore, persist, startup
    fun reregisterAllGeofences() {
        val database = AppDatabase.getDatabase(context)
        val repository = GeofenceRepository(database.geofenceDao())
        
        CoroutineScope(Dispatchers.IO).launch {
            val geofences = repository.getAllGeofences().first()
            geofences.forEach { geofence ->
                registerGeofence(
                    id = geofence.id,
                    latitude = geofence.latitude,
                    longitude = geofence.longitude,
                    radius = geofence.radius
                )
            }
        }
    }
    
    // tags: permission, check
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val backgroundLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation && backgroundLocation
    }
}

