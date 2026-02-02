package com.rex.careradius.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class for JOIN query results
 * Combines visit information with geofence details
 */
data class VisitWithGeofence(
    @Embedded val visit: VisitEntity,
    @Relation(
        parentColumn = "geofenceId",
        entityColumn = "id"
    )
    val geofence: GeofenceEntity
)
