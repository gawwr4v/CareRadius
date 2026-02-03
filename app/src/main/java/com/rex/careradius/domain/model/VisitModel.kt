package com.rex.careradius.domain.model

import com.rex.careradius.data.local.entity.VisitWithGeofence
import java.text.SimpleDateFormat
import java.util.*

/**
 * Domain model for Visit with Geofence information
 * Used for displaying visit history with formatted data
 */
data class VisitModel(
    val visitId: Long,
    val geofenceName: String,
    val entryTime: Long,
    val exitTime: Long?,
    val durationMillis: Long?,
    val geofenceLatitude: Double?,
    val geofenceLongitude: Double?,
    val isGeofenceDeleted: Boolean = false
) {
    val formattedEntryTime: String
        get() = formatTime(entryTime)
    
    val formattedExitTime: String
        get() = exitTime?.let { formatTime(it) } ?: "In Progress"
    
    val formattedDuration: String
        get() {
            if (durationMillis == null) return "--"
            val seconds = (durationMillis / 1000) % 60
            val minutes = (durationMillis / (1000 * 60)) % 60
            val hours = (durationMillis / (1000 * 60 * 60))
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    
    val formattedDate: String
        get() = formatDate(entryTime)
    
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    companion object {
        fun fromVisitWithGeofence(visitWithGeofence: VisitWithGeofence): VisitModel {
            val geofence = visitWithGeofence.geofence
            val visit = visitWithGeofence.visit
            
            return VisitModel(
                visitId = visit.id,
                // Use stored geofenceName from visit (preserved even after deletion)
                geofenceName = visit.geofenceName,
                entryTime = visit.entryTime,
                exitTime = visit.exitTime,
                durationMillis = visit.durationMillis,
                // Include location if geofence still exists
                geofenceLatitude = geofence?.latitude,
                geofenceLongitude = geofence?.longitude,
                isGeofenceDeleted = geofence == null
            )
        }
    }
}
