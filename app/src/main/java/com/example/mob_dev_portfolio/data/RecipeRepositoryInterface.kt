package com.example.mob_dev_portfolio.data

import kotlinx.coroutines.flow.Flow

interface RecipeRepositoryInterface {
    fun getDistinctAreasFlow(): Flow<List<String>>
    fun getRecipesFlow(
        query: String,
        area: String,
        sort: RecipeSort = RecipeSort.NameAsc
    ): Flow<List<RecipeSummaryView>>
    fun getFavouritesFlow(): Flow<List<RecipeSummaryView>>
    fun getShoppingListFlow(): Flow<List<ShoppingListItem>>
    fun getRecipeWithIngredientsFlow(recipeId: String): Flow<RecipeWithIngredients?>
    suspend fun insertRecipeWithIngredients(recipe: Recipe, ingredients: List<Ingredient>)
    suspend fun updateRecipeWithIngredients(recipe: Recipe, ingredients: List<Ingredient>)
    suspend fun deleteRecipeById(recipeId: String)
    suspend fun importRecipeFromUrl(url: String): Result<String>
    suspend fun createImportDraftFromUrl(url: String): Result<String>
    suspend fun consumeImportDraft(draftId: String): RecipeImportDraft?
    suspend fun updateShoppingItem(item: ShoppingListItem)
    suspend fun clearShoppingList()
    suspend fun clearCheckedShoppingItems()
    suspend fun addShoppingListItems(items: List<ShoppingListItem>)
    suspend fun setFavourite(recipeId: String, isFavourite: Boolean)
}
