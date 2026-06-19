package com.example.mob_dev_portfolio.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import com.example.mob_dev_portfolio.data.ShoppingListItem
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

data class ShoppingListSection(
    val sourceName: String,
    val items: List<ShoppingListItem>
)

sealed class ShoppingListUiState {
    data object Loading : ShoppingListUiState()
    data object Empty : ShoppingListUiState()
    data class Content(
        val items: List<ShoppingListItem>,
        val sections: List<ShoppingListSection>
    ) : ShoppingListUiState()
    data class Error(val message: String) : ShoppingListUiState()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: RecipeRepositoryInterface
) : ViewModel() {

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val retryRequests = MutableStateFlow(0)

    val uiState: StateFlow<ShoppingListUiState> = retryRequests
        .flatMapLatest {
            repository.getShoppingListFlow()
                .map<List<ShoppingListItem>, ShoppingListUiState> { shoppingItems ->
                    if (shoppingItems.isEmpty()) {
                        ShoppingListUiState.Empty
                    } else {
                        ShoppingListUiState.Content(
                            items = shoppingItems,
                            sections = shoppingItems.toSections()
                        )
                    }
                }
                .onStart { emit(ShoppingListUiState.Loading) }
                .catch { e ->
                    emit(ShoppingListUiState.Error(e.message ?: "Failed to load shopping list"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShoppingListUiState.Loading
        )

    val items: StateFlow<List<ShoppingListItem>> = uiState
        .map { state ->
            when (state) {
                is ShoppingListUiState.Content -> state.items
                ShoppingListUiState.Empty,
                is ShoppingListUiState.Error,
                ShoppingListUiState.Loading -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val sections: StateFlow<List<ShoppingListSection>> = uiState
        .map { state ->
            when (state) {
                is ShoppingListUiState.Content -> state.sections
                ShoppingListUiState.Empty,
                is ShoppingListUiState.Error,
                ShoppingListUiState.Loading -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun toggleChecked(item: ShoppingListItem) {
        viewModelScope.launch {
            try {
                repository.updateShoppingItem(item.copy(isChecked = !item.isChecked))
            } catch (e: Exception) {
                _errorEvents.emit(e.message ?: "Failed to update shopping item")
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                repository.clearShoppingList()
            } catch (e: Exception) {
                _errorEvents.emit(e.message ?: "Failed to clear shopping list")
            }
        }
    }

    fun clearChecked() {
        viewModelScope.launch {
            try {
                repository.clearCheckedShoppingItems()
            } catch (e: Exception) {
                _errorEvents.emit(e.message ?: "Failed to clear checked items")
            }
        }
    }

    fun retry() {
        retryRequests.value += 1
    }

    private fun List<ShoppingListItem>.toSections(): List<ShoppingListSection> {
        return groupBy { item ->
            item.sourceRecipeName.ifBlank { OTHER_ITEMS_SOURCE_NAME }
        }.map { (sourceName, sourceItems) ->
            ShoppingListSection(sourceName = sourceName, items = sourceItems)
        }
    }

    companion object {
        const val OTHER_ITEMS_SOURCE_NAME = "Other items"
    }
}
