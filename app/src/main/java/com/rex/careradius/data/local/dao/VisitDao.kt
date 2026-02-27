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

    // All visits with no exit time (potentially stale after Doze-mode EXIT drops)
    @Query("SELECT * FROM visits WHERE exitTime IS NULL")
    suspend fun getOpenVisits(): List<VisitEntity>
    
    @Transaction
    @Query("""
        SELECT * FROM visits
        ORDER BY entryTime DESC
    """)
    fun getAllVisitsWithGeofence(): Flow<List<VisitWithGeofence>>
    
    @Query("DELETE FROM visits WHERE id = :visitId")
    suspend fun deleteVisit(visitId: Long)
    
    @Query("DELETE FROM visits")
    suspend fun deleteAllVisits()
    
    @Query("DELETE FROM visits WHERE exitTime IS NOT NULL")
    suspend fun deleteCompletedVisits()
    
    @Query("""
        UPDATE visits 
        SET exitTime = :exitTime, durationMillis = :exitTime - entryTime 
        WHERE exitTime IS NULL
    """)
    suspend fun closeAllOpenVisits(exitTime: Long)
    
    // For JSON Import Merge deduplication
    @Query("""
        SELECT * FROM visits 
        WHERE geofenceName = :zoneName 
        AND entryTime = :entryTime 
        AND (exitTime = :exitTime OR (exitTime IS NULL AND :exitTime IS NULL)) 
        LIMIT 1
    """)
    suspend fun getVisitByAttributes(zoneName: String, entryTime: Long, exitTime: Long?): VisitEntity?
}
