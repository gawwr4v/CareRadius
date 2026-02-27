package com.rex.careradius.presentation.visitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rex.careradius.data.local.dao.VisitDao
import com.rex.careradius.data.local.entity.VisitWithGeofence
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Visit List
 * NOTE: Currently instanced via remember {} in NavGraph
 * Future Refactor: Convert to Hilt/Factory injections if app grows
 */
class VisitListViewModel(
    private val visitDao: VisitDao
) : ViewModel() {
    
    val visits: StateFlow<List<VisitWithGeofence>> = visitDao.getAllVisitsWithGeofence()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(60_000),
            initialValue = emptyList()
        )
    
    fun clearHistory() {
        viewModelScope.launch {
            visitDao.deleteCompletedVisits()
        }
    }
    
    fun deleteVisit(visitId: Long) {
        viewModelScope.launch {
            visitDao.deleteVisit(visitId)
        }
    }
}
