package com.example.mob_dev_portfolio.util

import com.example.mob_dev_portfolio.data.Ingredient
import com.example.mob_dev_portfolio.data.Recipe
import com.example.mob_dev_portfolio.data.RecipeImportDraft
import com.example.mob_dev_portfolio.data.RecipeRepositoryInterface
import com.example.mob_dev_portfolio.data.RecipeSort
import com.example.mob_dev_portfolio.data.RecipeSummaryView
import com.example.mob_dev_portfolio.data.RecipeWithIngredients
import com.example.mob_dev_portfolio.data.SettingsRepositoryInterface
import com.example.mob_dev_portfolio.data.ShoppingListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeRecipeRepository : RecipeRepositoryInterface {

    val recipeFlow = MutableStateFlow<RecipeWithIngredients?>(null)
    val recipesFlow = MutableStateFlow<List<RecipeSummaryView>>(emptyList())
    val favouritesFlow = MutableStateFlow<List<RecipeSummaryView>>(emptyList())
    val shoppingItemsFlow = MutableStateFlow<List<ShoppingListItem>>(emptyList())
    val areasFlow = MutableStateFlow<List<String>>(emptyList())
    var lastShoppingListItems: List<ShoppingListItem> = emptyList()
    var lastFavouriteSet: Pair<String, Boolean>? = null
    var insertedRecipe: Recipe? = null
    var insertedIngredients: List<Ingredient> = emptyList()
    var updatedRecipe: Recipe? = null
    var updatedIngredients: List<Ingredient> = emptyList()
    var deletedRecipeId: String? = null
    var updatedShoppingItem: ShoppingListItem? = null
    var didClearShoppingList = false
    var didClearCheckedShoppingItems = false
    var importRecipeResult: Result<String> = Result.success("imported-recipe")
    var importDraftResult: Result<String> = Result.success("draft-recipe")
    var importDraft: RecipeImportDraft? = null
    var addShoppingListItemsError: Exception? = null
    var setFavouriteError: Exception? = null
    var updateShoppingItemError: Exception? = null
    var clearShoppingListError: Exception? = null
    var clearCheckedShoppingItemsError: Exception? = null
    var getRecipesFlowError: Exception? = null
    var getFavouritesFlowError: Exception? = null
    var getShoppingListFlowError: Exception? = null
    var consumedDraftId: String? = null
    var importedUrl: String? = null
    var lastRecipeQuery: String? = null
    var lastRecipeArea: String? = null
    var lastRecipeSort: RecipeSort? = null

    override fun getDistinctAreasFlow(): Flow<List<String>> {
        return areasFlow
    }

    override fun getRecipesFlow(
        query: String,
        area: String,
        sort: RecipeSort
    ): Flow<List<RecipeSummaryView>> {
        lastRecipeQuery = query
        lastRecipeArea = area
        lastRecipeSort = sort
        getRecipesFlowError?.let { error ->
            return flow { throw error }
        }
        return recipesFlow
    }

    override fun getFavouritesFlow(): Flow<List<RecipeSummaryView>> {
        getFavouritesFlowError?.let { error ->
            return flow { throw error }
        }
        return favouritesFlow
    }

    override fun getShoppingListFlow(): Flow<List<ShoppingListItem>> {
        getShoppingListFlowError?.let { error ->
            return flow { throw error }
        }
        return shoppingItemsFlow
    }

    override fun getRecipeWithIngredientsFlow(recipeId: String): Flow<RecipeWithIngredients?> {
        return recipeFlow
    }

    override suspend fun insertRecipeWithIngredients(recipe: Recipe, ingredients: List<Ingredient>) {
        insertedRecipe = recipe
        insertedIngredients = ingredients
    }

    override suspend fun updateRecipeWithIngredients(recipe: Recipe, ingredients: List<Ingredient>) {
        updatedRecipe = recipe
        updatedIngredients = ingredients
    }

    override suspend fun deleteRecipeById(recipeId: String) {
        deletedRecipeId = recipeId
    }

    override suspend fun importRecipeFromUrl(url: String): Result<String> {
        importedUrl = url
        return importRecipeResult
    }

    override suspend fun createImportDraftFromUrl(url: String): Result<String> {
        importedUrl = url
        return importDraftResult
    }

    override suspend fun consumeImportDraft(draftId: String): RecipeImportDraft? {
        consumedDraftId = draftId
        return importDraft
    }

    override suspend fun updateShoppingItem(item: ShoppingListItem) {
        updateShoppingItemError?.let { throw it }
        updatedShoppingItem = item
    }

    override suspend fun clearShoppingList() {
        clearShoppingListError?.let { throw it }
        didClearShoppingList = true
    }

    override suspend fun clearCheckedShoppingItems() {
        clearCheckedShoppingItemsError?.let { throw it }
        didClearCheckedShoppingItems = true
    }

    override suspend fun addShoppingListItems(items: List<ShoppingListItem>) {
        addShoppingListItemsError?.let { throw it }
        lastShoppingListItems = items
    }

    override suspend fun setFavourite(recipeId: String, isFavourite: Boolean) {
        setFavouriteError?.let { throw it }
        lastFavouriteSet = recipeId to isFavourite
    }
}

class FakeSettingsRepository : SettingsRepositoryInterface {
    private val _useMetric = MutableStateFlow(true)
    private val _defaultPortions = MutableStateFlow(4)
    private val _onboardingComplete = MutableStateFlow(true)
    var setUseMetricError: Exception? = null
    var setDefaultPortionsError: Exception? = null
    var setOnboardingCompleteError: Exception? = null

    override val useMetric: Flow<Boolean> = _useMetric
    override val defaultPortions: Flow<Int> = _defaultPortions
    override val onboardingComplete: Flow<Boolean> = _onboardingComplete

    override suspend fun setUseMetric(value: Boolean) {
        setUseMetricError?.let { throw it }
        _useMetric.value = value
    }

    override suspend fun setDefaultPortions(value: Int) {
        setDefaultPortionsError?.let { throw it }
        _defaultPortions.value = value
    }

    override suspend fun setOnboardingComplete() {
        setOnboardingCompleteError?.let { throw it }
        _onboardingComplete.value = true
    }
}
