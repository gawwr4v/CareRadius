package com.rex.careradius.system.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.data.local.dao.VisitDao
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
        } catch (_: SecurityException) {
            // silently fail, geofence might already be removed
        }
    }
    
    // called on app startup to restore geofences after device restart
    // tags: restore, persist, startup
    fun reregisterAllGeofences() {
        CoroutineScope(Dispatchers.IO).launch {
            reregisterAllGeofencesSync()
        }
    }
    
    suspend fun reregisterAllGeofencesSync() {
        // Clear all OS-registered geofences associated with this PendingIntent first
        // Ensures a clean slate before re-registering from DB (critical for Replace mode imports)
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
        } catch (_: SecurityException) {
            // Ignore security exception if permissions were revoked
        }

        val database = AppDatabase.getDatabase(context)
        val geofenceDao = database.geofenceDao()
        
        val geofences = geofenceDao.getAllGeofences().first()
        geofences.forEach { geofence ->
            registerGeofence(
                id = geofence.id,
                latitude = geofence.latitude,
                longitude = geofence.longitude,
                radius = geofence.radius
            )
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

    // Called on app resume to fix visits that were left open because the OS dropped
    // EXIT broadcasts while in Doze mode (common on Samsung/Xiaomi with aggressive battery saving).
    // Strategy: get current location once, then close any open visit where the user is
    // measurably outside the zone radius.
    fun reconcileOpenVisitsOnStartup() {
        if (!hasLocationPermissions()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                // PRIORITY_BALANCED_POWER_ACCURACY avoids draining GPS – good enough for a geofence check
                // Tasks.await() is fine here because we are already on Dispatchers.IO
                val currentLocation = Tasks.await(
                    fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                ) ?: run {
                    android.util.Log.w("GeofenceManager", "reconcileOpenVisits: no location available")
                    return@launch
                }

                val database = AppDatabase.getDatabase(context)
                val visitDao: VisitDao = database.visitDao()
                val geofenceDao = database.geofenceDao()

                // Find all visits that have no exit time (currently "open")
                val openVisits = visitDao.getOpenVisits()
                android.util.Log.d("GeofenceManager", "reconcileOpenVisits: found ${openVisits.size} open visits")

                val now = System.currentTimeMillis()
                openVisits.forEach { visit ->
                    val geofence = visit.geofenceId?.let { geofenceDao.getGeofenceById(it) }
                        ?: geofenceDao.getGeofenceByName(visit.geofenceName)

                    if (geofence == null) {
                        // Zone was deleted — close the orphaned visit
                        android.util.Log.d("GeofenceManager", "reconcileOpenVisits: closing visit for deleted zone '${visit.geofenceName}'")
                        visitDao.updateVisit(visit.copy(exitTime = now, durationMillis = now - visit.entryTime))
                        return@forEach
                    }

                    val results = FloatArray(1)
                    Location.distanceBetween(
                        currentLocation.latitude, currentLocation.longitude,
                        geofence.latitude, geofence.longitude,
                        results
                    )
                    val distanceMeters = results[0]

                    if (distanceMeters > geofence.radius) {
                        // User is outside the zone. EXIT was missed. Close the visit now.
                        android.util.Log.d("GeofenceManager", "reconcileOpenVisits: closing stale visit for '${geofence.name}' (distance=${distanceMeters}m, radius=${geofence.radius}m)")
                        visitDao.updateVisit(visit.copy(exitTime = now, durationMillis = now - visit.entryTime))
                    } else {
                        android.util.Log.d("GeofenceManager", "reconcileOpenVisits: '${geofence.name}' still inside zone (distance=${distanceMeters}m)")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GeofenceManager", "reconcileOpenVisits failed: ${e.message}")
            }
        }
    }
}

