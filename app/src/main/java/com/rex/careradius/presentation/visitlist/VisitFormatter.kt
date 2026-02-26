package com.rex.careradius.presentation.visitlist

import com.rex.careradius.data.local.entity.VisitWithGeofence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension properties for UI formatting

val VisitWithGeofence.formattedEntryTime: String
    get() = formatTime(visit.entryTime)

val VisitWithGeofence.formattedExitTime: String
    get() = visit.exitTime?.let { formatTime(it) } ?: "Currently here"

val VisitWithGeofence.formattedDuration: String
    get() {
        val durationMillis = visit.durationMillis ?: return "--"
        val totalSeconds = durationMillis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return when {
            hours > 0 -> "Stayed ${hours}h ${minutes}m"
            minutes > 0 -> "Stayed ${minutes}m ${seconds}s"
            else -> "Stayed ${seconds}s"
        }
    }

val VisitWithGeofence.formattedDate: String
    get() = formatDate(visit.entryTime)

val VisitWithGeofence.isGeofenceDeleted: Boolean
    get() = geofence == null

val VisitWithGeofence.geofenceIcon: String
    get() = geofence?.icon ?: ""

val VisitWithGeofence.geofenceName: String
    get() = visit.geofenceName

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
