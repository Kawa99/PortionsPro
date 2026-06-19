package com.example.mob_dev_portfolio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_list ORDER BY added_at ASC, id ASC")
    fun getAll(): Flow<List<ShoppingListItem>>

    @Query(
        """
        SELECT * FROM shopping_list
        WHERE source_recipe_id = :recipeId
        ORDER BY added_at ASC, id ASC
        """
    )
    fun getItemsForRecipe(recipeId: String): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingListItem>)

    @Update
    suspend fun update(item: ShoppingListItem)

    @Query("DELETE FROM shopping_list")
    suspend fun clearAll()

    @Query("DELETE FROM shopping_list WHERE is_checked = 1")
    suspend fun clearCheckedItems()
}
