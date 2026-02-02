package com.rex.careradius.data.local.dao

import androidx.room.*
import com.rex.careradius.data.local.entity.GeofenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Geofence operations
 */
@Dao
interface GeofenceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(geofence: GeofenceEntity): Long
    
    @Query("SELECT * FROM geofences ORDER BY createdAt DESC")
    fun getAllGeofences(): Flow<List<GeofenceEntity>>
    
    @Query("SELECT * FROM geofences WHERE id = :geofenceId")
    suspend fun getGeofenceById(geofenceId: Long): GeofenceEntity?
    
    @Delete
    suspend fun delete(geofence: GeofenceEntity)
}
