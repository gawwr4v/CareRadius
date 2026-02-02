package com.rex.careradius.data.repository

import com.rex.careradius.data.local.dao.VisitDao
import com.rex.careradius.data.local.entity.VisitEntity
import com.rex.careradius.data.local.entity.VisitWithGeofence
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Visit operations
 * Handles entry/exit recording and visit history
 */
class VisitRepository(private val visitDao: VisitDao) {
    
    suspend fun insert(visit: VisitEntity): Long = visitDao.insert(visit)
    
    suspend fun updateVisit(visit: VisitEntity) = visitDao.updateVisit(visit)
    
    suspend fun getOpenVisitForGeofence(geofenceId: Long): VisitEntity? = 
        visitDao.getOpenVisitForGeofence(geofenceId)
    
    fun getAllVisitsWithGeofence(): Flow<List<VisitWithGeofence>> = 
        visitDao.getAllVisitsWithGeofence()
}
