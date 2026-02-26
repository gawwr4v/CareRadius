package com.rex.careradius.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rex.careradius.domain.usecase.ExportDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExportState {
    object Idle : ExportState
    object Loading : ExportState
    object Success : ExportState
    data class Error(val message: String) : ExportState
}

class ExportViewModel(
    private val exportDataUseCase: ExportDataUseCase
) : ViewModel() {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun exportData(destinationUri: Uri?) {
        if (destinationUri == null) {
            _exportState.value = ExportState.Error("Invalid file location selected.")
            return
        }

        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            
            // UseCase handles shifting to Dispatchers.IO internally
            val result = exportDataUseCase.execute(destinationUri)
            
            result.onSuccess {
                _exportState.value = ExportState.Success
            }.onFailure { exception ->
                _exportState.value = ExportState.Error(
                    exception.localizedMessage ?: "An unknown error occurred during export."
                )
            }
        }
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}

class ExportViewModelFactory(
    private val exportDataUseCase: ExportDataUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExportViewModel(exportDataUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
