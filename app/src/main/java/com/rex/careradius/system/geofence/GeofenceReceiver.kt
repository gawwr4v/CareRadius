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
    
    // tags: receive, event, trigger
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        
        if (geofencingEvent.hasError()) {
            return
        }
        
        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        
        val database = AppDatabase.getDatabase(context)
        val visitRepository = VisitRepository(database.visitDao())
        val notificationHelper = NotificationHelper(context)
        
        triggeringGeofences.forEach { geofence ->
            val geofenceId = geofence.requestId.toLongOrNull() ?: return@forEach
            
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    handleGeofenceEntry(context, geofenceId, visitRepository, notificationHelper)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
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
                return@launch
            }
            
            // create new visit record
            val visit = VisitEntity(
                geofenceId = geofenceId,
                entryTime = System.currentTimeMillis(),
                exitTime = null,
                durationMillis = null
            )
            visitRepository.insert(visit)
            
            // get geofence name for notification
            val database = AppDatabase.getDatabase(context)
            val geofence = database.geofenceDao().getGeofenceById(geofenceId)
            val geofenceName = geofence?.name ?: "Unknown"
            
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

