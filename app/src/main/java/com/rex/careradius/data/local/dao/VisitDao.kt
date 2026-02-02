package com.rex.careradius.data.local.dao

import androidx.room.*
import com.rex.careradius.data.local.entity.VisitEntity
import com.rex.careradius.data.local.entity.VisitWithGeofence
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Visit operations
 */
@Dao
interface VisitDao {
    
    @Insert
    suspend fun insert(visit: VisitEntity): Long
    
    @Update
    suspend fun updateVisit(visit: VisitEntity)
    
    @Query("SELECT * FROM visits WHERE geofenceId = :geofenceId AND exitTime IS NULL LIMIT 1")
    suspend fun getOpenVisitForGeofence(geofenceId: Long): VisitEntity?
    
    @Transaction
    @Query("""
        SELECT visits.* FROM visits
        INNER JOIN geofences ON visits.geofenceId = geofences.id
        ORDER BY visits.entryTime DESC
    """)
    fun getAllVisitsWithGeofence(): Flow<List<VisitWithGeofence>>
}
