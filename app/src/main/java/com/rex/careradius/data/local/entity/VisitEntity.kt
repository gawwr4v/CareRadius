package com.rex.careradius.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a Visit to a Geofence
 * Tracks entry/exit times and duration spent inside the geofence
 */
@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = GeofenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["geofenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("geofenceId")]
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val geofenceId: Long,
    val entryTime: Long, // timestamp when entered
    val exitTime: Long? = null, // timestamp when exited (null if still inside)
    val durationMillis: Long? = null // duration in milliseconds (null if still inside)
)
