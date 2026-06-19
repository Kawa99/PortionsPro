package com.example.mob_dev_portfolio.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import com.example.mob_dev_portfolio.data.RecipeSort
import com.example.mob_dev_portfolio.data.RecipeSummaryView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class RecipeListUiState {
    data object Loading : RecipeListUiState()
    data object Empty : RecipeListUiState()
    data class Content(val recipes: List<RecipeSummaryView>) : RecipeListUiState()
    data class Error(val message: String) : RecipeListUiState()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repository: RecipeRepositoryInterface
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedArea = MutableStateFlow("")
    val selectedArea: StateFlow<String> = _selectedArea.asStateFlow()

    private val _selectedSort = MutableStateFlow(RecipeSort.NameAsc)
    val selectedSort: StateFlow<RecipeSort> = _selectedSort.asStateFlow()

    private val retryRequests = MutableStateFlow(0)

    val areas: StateFlow<List<String>> = repository.getDistinctAreasFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<RecipeListUiState> = combine(
        _searchQuery.debounce(300L),
        _selectedArea,
        _selectedSort,
        retryRequests
    ) { query, area, sort, _ ->
        RecipeListFilters(query, area, sort)
    }
        .flatMapLatest { filters ->
            repository.getRecipesFlow(
                query = filters.query,
                area = filters.area,
                sort = filters.sort
            )
                .map<List<RecipeSummaryView>, RecipeListUiState> { recipes ->
                    if (recipes.isEmpty()) {
                        RecipeListUiState.Empty
                    } else {
                        RecipeListUiState.Content(recipes)
                    }
                }
                .onStart { emit(RecipeListUiState.Loading) }
                .catch { e ->
                    emit(RecipeListUiState.Error(e.message ?: "Failed to load recipes"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipeListUiState.Loading
        )

    val recipes: StateFlow<List<RecipeSummaryView>> = uiState
        .map { state ->
            when (state) {
                is RecipeListUiState.Content -> state.recipes
                RecipeListUiState.Empty,
                is RecipeListUiState.Error,
                RecipeListUiState.Loading -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onAreaSelected(area: String) {
        _selectedArea.value = area
    }

    fun onTimeSortPressed() {
        _selectedSort.value = when (_selectedSort.value) {
            RecipeSort.TimeAsc -> RecipeSort.TimeDesc
            RecipeSort.TimeDesc -> RecipeSort.NameAsc
            else -> RecipeSort.TimeAsc
        }
    }

    fun onIngredientSortPressed() {
        _selectedSort.value = when (_selectedSort.value) {
            RecipeSort.IngredientsAsc -> RecipeSort.IngredientsDesc
            RecipeSort.IngredientsDesc -> RecipeSort.NameAsc
            else -> RecipeSort.IngredientsAsc
        }
    }

    fun retry() {
        retryRequests.value += 1
    }
}

private data class RecipeListFilters(
    val query: String,
    val area: String,
    val sort: RecipeSort
)
