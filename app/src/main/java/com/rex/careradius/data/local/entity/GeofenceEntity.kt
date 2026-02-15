package com.rex.careradius.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Geofence
 * Stores location details, radius, and creation time
 */
@Entity(tableName = "geofences")
data class GeofenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float, // in meters (10-50m)
    val createdAt: Long, // timestamp
    val icon: String = "📍", // Emoji/icon for marker display
    val entryMessage: String = "", // Custom message on entry
    val exitMessage: String = "" // Custom message on exit
)
