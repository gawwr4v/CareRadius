package com.rex.careradius.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rex.careradius.domain.usecase.ImportDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportState {
    object Idle : ImportState
    data class AwaitingUserChoice(val uri: Uri) : ImportState
    object Importing : ImportState
    object Success : ImportState
    data class Error(val message: String) : ImportState
}

class ImportViewModel(
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun onFileSelected(uri: Uri?) {
        if (uri == null) {
            _importState.value = ImportState.Error("Invalid file location selected.")
            return
        }
        _importState.value = ImportState.AwaitingUserChoice(uri)
    }

    fun executeImport(uri: Uri, isReplaceMode: Boolean) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            
            // UseCase handles shifting to Dispatchers.IO internally
            val result = importDataUseCase.execute(uri, isReplaceMode)
            
            result.onSuccess {
                _importState.value = ImportState.Success
            }.onFailure { exception ->
                _importState.value = ImportState.Error(
                    exception.localizedMessage ?: "An unknown error occurred during import."
                )
            }
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }
}

class ImportViewModelFactory(
    private val importDataUseCase: ImportDataUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImportViewModel(importDataUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
