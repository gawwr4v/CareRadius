package com.rex.careradius.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class for JOIN query results
 * Combines visit information with geofence details
 * Geofence may be null if the geofence was deleted after the visit
 */
data class VisitWithGeofence(
    @Embedded val visit: VisitEntity,
    @Relation(
        parentColumn = "geofenceId",
        entityColumn = "id"
    )
    val geofence: GeofenceEntity? // nullable - geofence may have been deleted
)
