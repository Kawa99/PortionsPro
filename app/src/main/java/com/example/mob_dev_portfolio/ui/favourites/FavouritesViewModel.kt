package com.example.mob_dev_portfolio.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import com.example.mob_dev_portfolio.data.RecipeSummaryView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FavouritesUiState {
    data object Loading : FavouritesUiState()
    data object Empty : FavouritesUiState()
    data class Content(val favourites: List<RecipeSummaryView>) : FavouritesUiState()
    data class Error(val message: String) : FavouritesUiState()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val repository: RecipeRepositoryInterface
) : ViewModel() {

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val retryRequests = MutableStateFlow(0)

    val uiState: StateFlow<FavouritesUiState> = retryRequests
        .flatMapLatest {
            repository.getFavouritesFlow()
                .map<List<RecipeSummaryView>, FavouritesUiState> { favourites ->
                    if (favourites.isEmpty()) {
                        FavouritesUiState.Empty
                    } else {
                        FavouritesUiState.Content(favourites)
                    }
                }
                .onStart { emit(FavouritesUiState.Loading) }
                .catch { e ->
                    emit(FavouritesUiState.Error(e.message ?: "Failed to load favourites"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavouritesUiState.Loading
        )

    val favourites: StateFlow<List<RecipeSummaryView>> = uiState
        .map { state ->
            when (state) {
                is FavouritesUiState.Content -> state.favourites
                FavouritesUiState.Empty,
                is FavouritesUiState.Error,
                FavouritesUiState.Loading -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun unfavourite(recipeId: String) {
        viewModelScope.launch {
            try {
                repository.setFavourite(recipeId, isFavourite = false)
            } catch (e: Exception) {
                _errorEvents.emit(e.message ?: "Failed to update favourite")
            }
        }
    }

    fun retry() {
        retryRequests.value += 1
    }
}
