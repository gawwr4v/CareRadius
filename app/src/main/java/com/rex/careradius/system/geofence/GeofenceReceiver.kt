package com.rex.careradius.system.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.data.local.entity.VisitEntity
import com.rex.careradius.data.repository.VisitRepository
import com.rex.careradius.system.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// tags: broadcast, receiver, enter, exit, transition
class GeofenceReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "GeofenceReceiver"
    }
    
    // tags: receive, event, trigger
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(TAG, "onReceive called")
        
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            android.util.Log.e(TAG, "GeofencingEvent is null")
            return
        }
        
        if (geofencingEvent.hasError()) {
            android.util.Log.e(TAG, "GeofencingEvent error: ${geofencingEvent.errorCode}")
            return
        }
        
        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        
        android.util.Log.d(TAG, "Transition: $geofenceTransition, Geofences: ${triggeringGeofences?.size}")
        
        if (triggeringGeofences == null) {
            android.util.Log.e(TAG, "No triggering geofences")
            return
        }
        
        val database = AppDatabase.getDatabase(context)
        val visitRepository = VisitRepository(database.visitDao())
        val notificationHelper = NotificationHelper(context)
        
        triggeringGeofences.forEach { geofence ->
            val geofenceId = geofence.requestId.toLongOrNull() ?: return@forEach
            android.util.Log.d(TAG, "Processing geofence id: $geofenceId")
            
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    android.util.Log.d(TAG, "ENTER transition for $geofenceId")
                    handleGeofenceEntry(context, geofenceId, visitRepository, notificationHelper)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    android.util.Log.d(TAG, "EXIT transition for $geofenceId")
                    handleGeofenceExit(context, geofenceId, visitRepository, notificationHelper)
                }
            }
        }
    }
    
    // tags: enter, entry, visit, start
    private fun handleGeofenceEntry(
        context: Context,
        geofenceId: Long,
        visitRepository: VisitRepository,
        notificationHelper: NotificationHelper
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            // check for duplicate entry (gps jitter edge case)
            val openVisit = visitRepository.getOpenVisitForGeofence(geofenceId)
            if (openVisit != null) {
                android.util.Log.d(TAG, "Already have open visit for $geofenceId, skipping")
                return@launch
            }
            
            // get geofence details first for name
            val database = AppDatabase.getDatabase(context)
            val geofence = database.geofenceDao().getGeofenceById(geofenceId)
            val geofenceName = geofence?.name ?: "Unknown"
            
            // create new visit record with geofence name preserved
            val visit = VisitEntity(
                geofenceId = geofenceId,
                geofenceName = geofenceName,
                entryTime = System.currentTimeMillis(),
                exitTime = null,
                durationMillis = null
            )
            visitRepository.insert(visit)
            android.util.Log.d(TAG, "Created visit for $geofenceName (id=$geofenceId)")
            
            notificationHelper.showGeofenceNotification(
                geofenceName = geofenceName,
                eventType = "Entered",
                notificationId = geofenceId.toInt()
            )
        }
    }
    
    // tags: exit, leave, visit, end, duration
    private fun handleGeofenceExit(
        context: Context,
        geofenceId: Long,
        visitRepository: VisitRepository,
        notificationHelper: NotificationHelper
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val openVisit = visitRepository.getOpenVisitForGeofence(geofenceId)
            if (openVisit == null) {
                return@launch
            }
            
            // calculate duration and close visit
            val exitTime = System.currentTimeMillis()
            val duration = exitTime - openVisit.entryTime
            
            val updatedVisit = openVisit.copy(
                exitTime = exitTime,
                durationMillis = duration
            )
            visitRepository.updateVisit(updatedVisit)
            
            val database = AppDatabase.getDatabase(context)
            val geofence = database.geofenceDao().getGeofenceById(geofenceId)
            val geofenceName = geofence?.name ?: "Unknown"
            
            notificationHelper.showGeofenceNotification(
                geofenceName = geofenceName,
                eventType = "Exited",
                notificationId = geofenceId.toInt() + 10000
            )
        }
    }
}

