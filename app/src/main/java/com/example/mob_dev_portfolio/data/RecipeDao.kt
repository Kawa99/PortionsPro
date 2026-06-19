package com.example.mob_dev_portfolio.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipe_summary ORDER BY name ASC")
    fun getAllSummaries(): Flow<List<RecipeSummaryView>>

    @Query(
        """
        SELECT * FROM recipe_summary
        WHERE (:area = '' OR area = :area)
          AND (:query = '' OR name LIKE '%' || :query || '%')
        ORDER BY
          CASE WHEN :sortKey = 'time' AND :ascending = 1 THEN total_time_minutes END ASC,
          CASE WHEN :sortKey = 'time' AND :ascending = 0 THEN total_time_minutes END DESC,
          CASE WHEN :sortKey = 'ingredients' AND :ascending = 1 THEN ingredient_count END ASC,
          CASE WHEN :sortKey = 'ingredients' AND :ascending = 0 THEN ingredient_count END DESC,
          name ASC
        """
    )
    fun getSummaries(
        query: String,
        area: String,
        sortKey: String,
        ascending: Boolean
    ): Flow<List<RecipeSummaryView>>

    @Query("SELECT * FROM recipe_summary WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchSummaries(query: String): Flow<List<RecipeSummaryView>>

    @Query("SELECT DISTINCT area FROM recipes WHERE area != '' ORDER BY area ASC")
    fun getDistinctAreasFlow(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    fun getRecipeWithIngredients(recipeId: String): Flow<RecipeWithIngredients?>

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): Recipe?

    @Query("SELECT * FROM recipes WHERE source_url = :sourceUrl COLLATE NOCASE LIMIT 1")
    suspend fun getRecipeBySourceUrl(sourceUrl: String): Recipe?

    @Query("SELECT * FROM recipe_summary WHERE is_favourite = 1 ORDER BY name ASC")
    fun getFavourites(): Flow<List<RecipeSummaryView>>

    @Query(
        """
        UPDATE recipes
        SET is_favourite = :isFav,
            updated_at = :updatedAt
        WHERE id = :recipeId
        """
    )
    suspend fun setFavourite(
        recipeId: String,
        isFav: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<Recipe>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<Ingredient>)

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("DELETE FROM ingredients WHERE recipe_id = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Query("SELECT COUNT(*) FROM recipes WHERE is_user_created = 0")
    suspend fun getCachedRecipeCount(): Int
}
