package com.rex.careradius.domain.model

import com.rex.careradius.data.local.entity.GeofenceEntity

/**
 * Domain model for Geofence
 * Represents a geofence in the business logic layer
 */
data class GeofenceModel(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val createdAt: Long
) {
    companion object {
        fun fromEntity(entity: GeofenceEntity): GeofenceModel = GeofenceModel(
            id = entity.id,
            name = entity.name,
            latitude = entity.latitude,
            longitude = entity.longitude,
            radius = entity.radius,
            createdAt = entity.createdAt
        )
    }
    
    fun toEntity(): GeofenceEntity = GeofenceEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        createdAt = createdAt
    )
}
