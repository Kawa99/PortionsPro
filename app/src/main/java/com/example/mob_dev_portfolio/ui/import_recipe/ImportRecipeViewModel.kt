package com.example.mob_dev_portfolio.ui.import_recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportUiState {
    data object Idle : ImportUiState()
    data object Loading : ImportUiState()
    data class Success(val draftId: String) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

@HiltViewModel
class ImportRecipeViewModel @Inject constructor(
    private val repository: RecipeRepositoryInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    fun onUrlChanged(value: String) {
        _url.value = value
        if (_uiState.value is ImportUiState.Error) {
            _uiState.value = ImportUiState.Idle
        }
    }

    fun importFromUrl() {
        val urlToImport = _url.value.trim()
        if (urlToImport.isBlank()) {
            _uiState.value = ImportUiState.Error("Please enter a URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = ImportUiState.Loading
            try {
                val result = repository.createImportDraftFromUrl(urlToImport)
                _uiState.value = result.fold(
                    onSuccess = { ImportUiState.Success(it) },
                    onFailure = { ImportUiState.Error(it.message ?: "Import failed") }
                )
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun markNavigationHandled() {
        _uiState.value = ImportUiState.Idle
        _url.value = ""
    }
}
