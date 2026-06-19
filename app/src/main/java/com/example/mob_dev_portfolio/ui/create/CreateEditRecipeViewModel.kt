package com.example.mob_dev_portfolio.ui.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mob_dev_portfolio.data.Recipe
import com.example.mob_dev_portfolio.data.RecipeImportDraft
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import com.example.mob_dev_portfolio.data.RecipeWithIngredients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale
import javax.inject.Inject

sealed class CreateEditUiState {
    object Idle : CreateEditUiState()
    object Saving : CreateEditUiState()
    data class Saved(val recipeId: String) : CreateEditUiState()
    object Deleted : CreateEditUiState()
    data class Error(val message: String) : CreateEditUiState()
}

@HiltViewModel
class CreateEditRecipeViewModel @Inject constructor(
    private val repository: RecipeRepositoryInterface,
    private val imageStorage: RecipeImageStorageInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateEditUiState>(CreateEditUiState.Idle)
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    private val _loadedRecipe = MutableStateFlow<RecipeWithIngredients?>(null)
    val loadedRecipe: StateFlow<RecipeWithIngredients?> = _loadedRecipe.asStateFlow()

    private val _formState = MutableStateFlow(CreateEditRecipeFormState())
    val formState: StateFlow<CreateEditRecipeFormState> = _formState.asStateFlow()

    private val _formErrors = MutableStateFlow(CreateEditFormErrors())
    val formErrors: StateFlow<CreateEditFormErrors> = _formErrors.asStateFlow()

    private var loadJob: Job? = null
    private var currentRecipeId: String? = null
    private var appliedLoadedRecipeId: String? = null
    private val appliedImportDraftIds = mutableSetOf<String>()

    fun importRecipeImage(sourceUri: Uri) {
        viewModelScope.launch {
            try {
                val copiedImage = imageStorage.copyToInternalStorage(sourceUri)
                if (copiedImage.isNotBlank()) {
                    onThumbnailUrlChanged(copiedImage)
                }
            } catch (e: Exception) {
                _uiState.value = CreateEditUiState.Error(e.message ?: "Image import failed")
            }
        }
    }

    fun loadImportDraft(draftId: String) {
        if (draftId in appliedImportDraftIds) return
        viewModelScope.launch {
            try {
                val draft = consumeImportDraft(draftId) ?: return@launch
                applyImportDraft(draft)
                appliedImportDraftIds += draftId
            } catch (e: Exception) {
                _uiState.value = CreateEditUiState.Error(e.message ?: "Import draft failed")
            }
        }
    }

    private suspend fun consumeImportDraft(draftId: String): RecipeImportDraft? {
        val draft = repository.consumeImportDraft(draftId) ?: return null
        val thumbnailUrl = draft.recipe.thumbnailUrl
        val remoteThumbnailUrl = thumbnailUrl.toHttpUrlOrNull()
        if (remoteThumbnailUrl == null) {
            return draft
        }
        if (!remoteThumbnailUrl.isHttps) {
            return draft.copy(recipe = draft.recipe.copy(thumbnailUrl = ""))
        }

        val localThumbnailUrl = imageStorage.downloadToInternalStorage(thumbnailUrl)
        return if (localThumbnailUrl.isNotBlank()) {
            draft.copy(recipe = draft.recipe.copy(thumbnailUrl = localThumbnailUrl))
        } else {
            draft
        }
    }

    fun loadRecipe(recipeId: String) {
        currentRecipeId = recipeId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getRecipeWithIngredientsFlow(recipeId).collectLatest { result ->
                _loadedRecipe.value = result
                if (result != null && appliedLoadedRecipeId != result.recipe.id) {
                    applyLoadedRecipe(result)
                    appliedLoadedRecipeId = result.recipe.id
                }
            }
        }
    }

    fun saveRecipe(recipeId: String): CreateEditFormErrors {
        val formState = _formState.value
        val errors = formState.validate()
        _formErrors.value = errors
        if (errors.hasErrors) return errors
        val preparedForm = formState.toPreparedRecipeForm()

        viewModelScope.launch {
            _uiState.value = CreateEditUiState.Saving
            try {
                val savedRecipeId: String
                if (recipeId == "NEW") {
                    val id = java.util.UUID.randomUUID().toString()
                    currentRecipeId = id
                    savedRecipeId = id
                    val recipe = Recipe(
                        id = id,
                        name = preparedForm.name,
                        category = preparedForm.category,
                        area = preparedForm.area,
                        instructions = preparedForm.instructions,
                        thumbnailUrl = preparedForm.thumbnailUrl,
                        sourceUrl = preparedForm.sourceUrl,
                        baseServings = preparedForm.baseServings,
                        prepTimeMinutes = preparedForm.prepTimeMinutes,
                        cookTimeMinutes = preparedForm.cookTimeMinutes,
                        isUserCreated = true,
                        isCached = false
                    )
                    repository.insertRecipeWithIngredients(recipe, preparedForm.ingredients.mapIndexed { index, ing ->
                        ing.copy(recipeId = id, displayOrder = index)
                    })
                } else {
                    currentRecipeId = recipeId
                    savedRecipeId = recipeId
                    val existing = _loadedRecipe.value?.recipe
                        ?: throw IllegalStateException("Recipe not loaded")
                    val updated = existing.copy(
                        name = preparedForm.name,
                        category = preparedForm.category,
                        area = preparedForm.area,
                        instructions = preparedForm.instructions,
                        thumbnailUrl = preparedForm.thumbnailUrl,
                        sourceUrl = preparedForm.sourceUrl.ifBlank { existing.sourceUrl },
                        baseServings = preparedForm.baseServings,
                        prepTimeMinutes = preparedForm.prepTimeMinutes,
                        cookTimeMinutes = preparedForm.cookTimeMinutes,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateRecipeWithIngredients(
                        updated,
                        preparedForm.ingredients.mapIndexed { index, ing ->
                            ing.copy(recipeId = recipeId, displayOrder = index)
                        }
                    )
                }
                _uiState.value = CreateEditUiState.Saved(savedRecipeId)
            } catch (e: Exception) {
                _uiState.value = CreateEditUiState.Error(e.message ?: "Save failed")
            }
        }

        return CreateEditFormErrors()
    }

    fun onNameChanged(value: String) {
        _formState.update { it.copy(name = value) }
        if (_formErrors.value.name) {
            _formErrors.update { it.copy(name = false) }
        }
    }

    fun onCategoryChanged(value: String) {
        _formState.update { it.copy(category = value) }
    }

    fun onBaseServingsChanged(value: String) {
        _formState.update { it.copy(baseServings = value) }
        if (_formErrors.value.servings) {
            _formErrors.update { it.copy(servings = false) }
        }
    }

    fun onThumbnailUrlChanged(value: String) {
        _formState.update { it.copy(thumbnailUrl = value) }
    }

    fun addIngredientRow() {
        _formState.update { it.copy(ingredientRows = it.ingredientRows + IngredientInputRow()) }
    }

    fun onIngredientNameChanged(index: Int, value: String) {
        updateIngredientRow(index) { it.copy(name = value) }
    }

    fun onIngredientQuantityChanged(index: Int, value: String) {
        updateIngredientRow(index) { it.copy(quantity = value) }
        _formErrors.update {
            it.copy(ingredientQuantityErrorIndexes = it.ingredientQuantityErrorIndexes - index)
        }
    }

    fun onIngredientUnitChanged(index: Int, value: String) {
        updateIngredientRow(index) { it.copy(unit = value) }
    }

    fun removeIngredientRow(index: Int) {
        val currentRows = _formState.value.ingredientRows
        if (index !in currentRows.indices) return
        val nextRows = currentRows.toMutableList().also {
            it.removeAt(index)
            if (it.isEmpty()) it.add(IngredientInputRow())
        }
        _formState.update { it.copy(ingredientRows = nextRows) }
        _formErrors.update { errors ->
            errors.copy(
                ingredientQuantityErrorIndexes = errors.ingredientQuantityErrorIndexes
                    .mapNotNull { errorIndex ->
                        when {
                            errorIndex < index -> errorIndex
                            errorIndex > index -> errorIndex - 1
                            else -> null
                        }
                    }
                    .toSet()
            )
        }
    }

    fun addInstructionRow() {
        _formState.update { it.copy(instructionRows = it.instructionRows + InstructionInputRow()) }
    }

    fun onInstructionChanged(index: Int, value: String) {
        val currentRows = _formState.value.instructionRows
        if (index !in currentRows.indices) return
        val nextRows = currentRows.toMutableList().also {
            it[index] = it[index].copy(text = value)
        }
        _formState.update { it.copy(instructionRows = nextRows) }
    }

    fun removeInstructionRow(index: Int) {
        val currentRows = _formState.value.instructionRows
        if (index !in currentRows.indices) return
        val nextRows = currentRows.toMutableList().also {
            it.removeAt(index)
            if (it.isEmpty()) it.add(InstructionInputRow())
        }
        _formState.update { it.copy(instructionRows = nextRows) }
    }

    fun deleteRecipe() {
        viewModelScope.launch {
            _uiState.value = CreateEditUiState.Saving
            try {
                val recipeId = currentRecipeId ?: throw IllegalStateException("Recipe not loaded")
                repository.deleteRecipeById(recipeId)
                _uiState.value = CreateEditUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = CreateEditUiState.Error(e.message ?: "Delete failed")
            }
        }
    }

    fun resetUiState() {
        if (_uiState.value != CreateEditUiState.Saving) {
            _uiState.value = CreateEditUiState.Idle
        }
    }

    private fun updateIngredientRow(index: Int, transform: (IngredientInputRow) -> IngredientInputRow) {
        val currentRows = _formState.value.ingredientRows
        if (index !in currentRows.indices) return
        val nextRows = currentRows.toMutableList().also {
            it[index] = transform(it[index])
        }
        _formState.update { it.copy(ingredientRows = nextRows) }
    }

    private fun applyLoadedRecipe(data: RecipeWithIngredients) {
        currentRecipeId = data.recipe.id
        _formState.value = data.toFormState()
        _formErrors.value = CreateEditFormErrors()
    }

    private fun applyImportDraft(draft: RecipeImportDraft) {
        _formState.value = RecipeWithIngredients(
            recipe = draft.recipe,
            ingredients = draft.ingredients
        ).toFormState()
        _formErrors.value = CreateEditFormErrors()
    }

    private fun RecipeWithIngredients.toFormState(): CreateEditRecipeFormState {
        return CreateEditRecipeFormState(
            name = recipe.name,
            category = recipe.category,
            baseServings = recipe.baseServings.toString(),
            instructionRows = recipe.instructions
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { InstructionInputRow(it) }
                .ifEmpty { listOf(InstructionInputRow()) },
            thumbnailUrl = recipe.thumbnailUrl,
            ingredientRows = ingredients.sortedBy { it.displayOrder }.map {
                IngredientInputRow(
                    name = it.name,
                    quantity = if (it.quantity > 0.0) {
                        String.format(Locale.US, "%.2f", it.quantity).trimEnd('0').trimEnd('.')
                    } else {
                        ""
                    },
                    unit = it.unit
                )
            }.ifEmpty { listOf(IngredientInputRow()) },
            area = recipe.area,
            prepTimeMinutes = recipe.prepTimeMinutes,
            cookTimeMinutes = recipe.cookTimeMinutes,
            sourceUrl = recipe.sourceUrl
        )
    }
}
