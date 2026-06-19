package com.example.mob_dev_portfolio.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mob_dev_portfolio.data.Ingredient
import com.example.mob_dev_portfolio.data.Recipe
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import com.example.mob_dev_portfolio.data.SettingsRepositoryInterface
import com.example.mob_dev_portfolio.data.ShoppingListItem
import com.example.mob_dev_portfolio.util.CanonicalUnit
import com.example.mob_dev_portfolio.util.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScaledIngredient(
    val name: String,
    val scaledQuantity: Double,
    val unit: String,
    val hasQuantity: Boolean = true
)

data class InstructionStepUi(
    val id: String,
    val text: String
)

sealed class RecipeDetailUiState {
    object Loading : RecipeDetailUiState()
    data class Success(
        val recipe: Recipe,
        val portions: Int,
        val scaledIngredients: List<ScaledIngredient>,
        val instructionSteps: List<InstructionStepUi>
    ) : RecipeDetailUiState()
    data class Error(val message: String) : RecipeDetailUiState()
}

sealed class RecipeDetailEvent {
    data object ShoppingListAddSucceeded : RecipeDetailEvent()
    data class ShoppingListAddFailed(val message: String) : RecipeDetailEvent()
    data class FavouriteUpdateFailed(val message: String) : RecipeDetailEvent()
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepositoryInterface,
    private val settingsRepository: SettingsRepositoryInterface
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])

    private val _uiState = MutableStateFlow<RecipeDetailUiState>(RecipeDetailUiState.Loading)
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val _portions = MutableStateFlow(2)
    val portions: StateFlow<Int> = _portions.asStateFlow()

    private val _checkedSteps = MutableStateFlow<Set<Int>>(emptySet())
    val checkedSteps: StateFlow<Set<Int>> = _checkedSteps.asStateFlow()

    private val _events = MutableSharedFlow<RecipeDetailEvent>()
    val events: SharedFlow<RecipeDetailEvent> = _events.asSharedFlow()

    private var currentRecipe: Recipe? = null
    private var baseIngredients: List<Ingredient> = emptyList()
    private var hasUserAdjustedPortions = false
    private var useMetric = true

    init {
        viewModelScope.launch {
            settingsRepository.defaultPortions.collect { defaultPortions ->
                if (!hasUserAdjustedPortions) {
                    _portions.value = defaultPortions.coerceIn(1, 20)
                    recalculate()
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.useMetric.collect { useMetricPreference ->
                useMetric = useMetricPreference
                recalculate()
            }
        }

        viewModelScope.launch {
            repository.getRecipeWithIngredientsFlow(recipeId).collect { recipeWithIngredients ->
                if (recipeWithIngredients == null) {
                    currentRecipe = null
                    baseIngredients = emptyList()
                    _checkedSteps.value = emptySet()
                    _uiState.value = RecipeDetailUiState.Error("Recipe not found")
                } else {
                    if (currentRecipe?.id != recipeWithIngredients.recipe.id) {
                        _checkedSteps.value = emptySet()
                    }
                    currentRecipe = recipeWithIngredients.recipe
                    baseIngredients = recipeWithIngredients.ingredients.sortedBy { it.displayOrder }
                    recalculate()
                }
            }
        }
    }

    fun setPortions(newPortions: Int) {
        hasUserAdjustedPortions = true
        _portions.value = newPortions.coerceIn(1, 20)
        recalculate()
    }

    fun toggleFavourite() {
        val recipe = currentRecipe ?: return

        viewModelScope.launch {
            try {
                repository.setFavourite(recipeId, !recipe.isFavourite)
            } catch (e: Exception) {
                _events.emit(
                    RecipeDetailEvent.FavouriteUpdateFailed(
                        e.message ?: "Failed to update favourite"
                    )
                )
            }
        }
    }

    fun addToShoppingList() {
        val state = _uiState.value as? RecipeDetailUiState.Success ?: return
        val shoppingListItems = state.scaledIngredients.map { ingredient ->
            ShoppingListItem(
                ingredientName = ingredient.name,
                quantity = if (ingredient.hasQuantity) ingredient.scaledQuantity else 0.0,
                unit = if (ingredient.hasQuantity) ingredient.unit else "",
                sourceRecipeId = state.recipe.id,
                sourceRecipeName = state.recipe.name
            )
        }

        viewModelScope.launch {
            try {
                repository.addShoppingListItems(shoppingListItems)
                _events.emit(RecipeDetailEvent.ShoppingListAddSucceeded)
            } catch (e: Exception) {
                _events.emit(
                    RecipeDetailEvent.ShoppingListAddFailed(
                        e.message ?: "Failed to add ingredients to shopping list"
                    )
                )
            }
        }
    }

    fun toggleStep(stepIndex: Int) {
        _checkedSteps.update { checkedSteps ->
            if (stepIndex in checkedSteps) checkedSteps - stepIndex else checkedSteps + stepIndex
        }
    }

    private fun recalculate() {
        val recipe = currentRecipe ?: return
        val scaleFactor = _portions.value.toDouble() / recipe.baseServings.toDouble()
        val scaledIngredients = baseIngredients.map { ingredient ->
            val hasQuantity = ingredient.quantity > 0.0
            val scaledQuantity = ingredient.quantity * scaleFactor
            val fromUnit = CanonicalUnit.fromSymbol(ingredient.unit)
            val (convertedQuantity, displayUnit) = UnitConverter.convert(
                quantity = scaledQuantity,
                from = fromUnit,
                useMetric = useMetric
            )

            ScaledIngredient(
                name = ingredient.name,
                scaledQuantity = if (hasQuantity) convertedQuantity else 0.0,
                unit = if (hasQuantity) displayUnit.symbol else "",
                hasQuantity = hasQuantity
            )
        }

        _uiState.value = RecipeDetailUiState.Success(
            recipe = recipe,
            portions = _portions.value,
            scaledIngredients = scaledIngredients,
            instructionSteps = recipe.instructions.toInstructionSteps()
        )
    }

    private fun String.toInstructionSteps(): List<InstructionStepUi> {
        return split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { index, text ->
                InstructionStepUi(id = "$index-$text", text = text)
            }
    }
}
