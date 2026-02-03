package com.rex.careradius.presentation.visitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rex.careradius.data.repository.VisitRepository
import com.rex.careradius.domain.model.VisitModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Visit List Screen
 * Displays visit history with geofence details
 */
class VisitListViewModel(
    private val visitRepository: VisitRepository
) : ViewModel() {
    
    val visits: StateFlow<List<VisitModel>> = visitRepository.getAllVisitsWithGeofence()
        .map { visitList ->
            visitList.map { VisitModel.fromVisitWithGeofence(it) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun clearHistory() {
        viewModelScope.launch {
            visitRepository.clearAllVisits()
        }
    }
    
    fun deleteVisit(visitId: Long) {
        viewModelScope.launch {
            visitRepository.deleteVisit(visitId)
        }
    }
}
