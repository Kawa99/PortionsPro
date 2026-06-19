package com.example.mob_dev_portfolio.data

import androidx.room.withTransaction
import com.example.mob_dev_portfolio.data.remote.RecipeUrlImportService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecipeRepository @Inject constructor(
    private val database: AppDatabase,
    private val importService: RecipeUrlImportService,
    private val importDraftStore: RecipeImportDraftStore
) : RecipeRepositoryInterface {
    private val recipeDao = database.recipeDao()
    private val shoppingListDao = database.shoppingListDao()

    override fun getDistinctAreasFlow(): Flow<List<String>> {
        return recipeDao.getDistinctAreasFlow()
    }

    override fun getRecipesFlow(
        query: String,
        area: String,
        sort: RecipeSort
    ): Flow<List<RecipeSummaryView>> {
        return recipeDao.getSummaries(
            query = query.trim(),
            area = area.trim(),
            sortKey = sort.sortKey,
            ascending = sort.ascending
        )
    }

    fun getAllRecipesFlow(): Flow<List<RecipeSummaryView>> {
        return recipeDao.getAllSummaries()
    }

    override fun getRecipeWithIngredientsFlow(recipeId: String): Flow<RecipeWithIngredients?> {
        return recipeDao.getRecipeWithIngredients(recipeId)
    }

    override suspend fun insertRecipeWithIngredients(recipe: Recipe, ingredients: List<Ingredient>) {
        database.withTransaction {
            recipeDao.insertRecipe(recipe)
            recipeDao.insertIngredients(ingredients)
        }
    }

    override suspend fun updateRecipeWithIngredients(recipe: Recipe, ingredients: List<Ingredient>) {
        database.withTransaction {
            recipeDao.updateRecipe(recipe)
            recipeDao.deleteIngredients(recipe.id)
            recipeDao.insertIngredients(ingredients)
        }
    }

    override suspend fun deleteRecipeById(recipeId: String) {
        val recipe = recipeDao.getRecipeById(recipeId) ?: return
        recipeDao.deleteRecipe(recipe)
    }

    override suspend fun importRecipeFromUrl(url: String): Result<String> {
        return when (val result = importService.importFromUrl(url)) {
            is RecipeUrlImportService.ImportResult.Success -> {
                if (recipeAlreadyExists(result.recipe.sourceUrl)) {
                    return duplicateImportFailure()
                }
                runCatching {
                    insertRecipeWithIngredients(result.recipe, result.ingredients)
                    result.recipe.id
                }.recoverCatching { error ->
                    throw Exception("Failed to save recipe: ${error.message}", error)
                }
            }

            RecipeUrlImportService.ImportResult.NoSchemaFound -> {
                Result.failure(Exception("No supported recipe metadata was found on this page."))
            }

            is RecipeUrlImportService.ImportResult.NetworkError -> {
                Result.failure(Exception("Network error: ${result.message}"))
            }

            RecipeUrlImportService.ImportResult.InvalidUrl -> {
                Result.failure(Exception("Invalid URL. Make sure it starts with https://"))
            }
        }
    }

    override suspend fun createImportDraftFromUrl(url: String): Result<String> {
        return when (val result = importService.importFromUrl(url)) {
            is RecipeUrlImportService.ImportResult.Success -> {
                if (recipeAlreadyExists(result.recipe.sourceUrl)) {
                    return duplicateImportFailure()
                }
                Result.success(
                    importDraftStore.put(
                        RecipeImportDraft(
                            recipe = result.recipe,
                            ingredients = result.ingredients
                        )
                    )
                )
            }

            RecipeUrlImportService.ImportResult.NoSchemaFound -> {
                Result.failure(Exception("No supported recipe metadata was found on this page."))
            }

            is RecipeUrlImportService.ImportResult.NetworkError -> {
                Result.failure(Exception("Network error: ${result.message}"))
            }

            RecipeUrlImportService.ImportResult.InvalidUrl -> {
                Result.failure(Exception("Invalid URL. Make sure it starts with https://"))
            }
        }
    }

    override suspend fun consumeImportDraft(draftId: String): RecipeImportDraft? {
        return importDraftStore.consume(draftId)
    }

    private suspend fun recipeAlreadyExists(sourceUrl: String): Boolean {
        return sourceUrl.isNotBlank() && recipeDao.getRecipeBySourceUrl(sourceUrl) != null
    }

    private fun duplicateImportFailure(): Result<String> {
        return Result.failure(Exception("This recipe has already been imported."))
    }

    override fun getFavouritesFlow(): Flow<List<RecipeSummaryView>> {
        return recipeDao.getFavourites()
    }

    fun searchRecipesFlow(query: String): Flow<List<RecipeSummaryView>> {
        return recipeDao.searchSummaries(query)
    }

    override suspend fun setFavourite(recipeId: String, isFavourite: Boolean) {
        recipeDao.setFavourite(recipeId, isFavourite)
    }

    override fun getShoppingListFlow(): Flow<List<ShoppingListItem>> {
        return shoppingListDao.getAll()
    }

    override suspend fun updateShoppingItem(item: ShoppingListItem) {
        shoppingListDao.update(item)
    }

    override suspend fun clearShoppingList() {
        shoppingListDao.clearAll()
    }

    override suspend fun clearCheckedShoppingItems() {
        shoppingListDao.clearCheckedItems()
    }

    override suspend fun addShoppingListItems(items: List<ShoppingListItem>) {
        shoppingListDao.insertAll(items)
    }
}
