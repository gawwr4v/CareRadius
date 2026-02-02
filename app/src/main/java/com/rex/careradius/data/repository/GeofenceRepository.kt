package com.rex.careradius.data.repository

import com.rex.careradius.data.local.dao.GeofenceDao
import com.rex.careradius.data.local.entity.GeofenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Geofence operations
 * Abstracts data access layer from ViewModels
 */
class GeofenceRepository(private val geofenceDao: GeofenceDao) {
    
    fun getAllGeofences(): Flow<List<GeofenceEntity>> = geofenceDao.getAllGeofences()
    
    suspend fun insert(geofence: GeofenceEntity): Long = geofenceDao.insert(geofence)
    
    suspend fun getGeofenceById(id: Long): GeofenceEntity? = geofenceDao.getGeofenceById(id)
    
    suspend fun delete(geofence: GeofenceEntity) = geofenceDao.delete(geofence)
}
