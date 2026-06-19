package com.example.mob_dev_portfolio.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "ingredient_name")
    val ingredientName: String,

    @ColumnInfo(name = "quantity")
    val quantity: Double,

    @ColumnInfo(name = "unit")
    val unit: String = "",

    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,

    @ColumnInfo(name = "source_recipe_id")
    val sourceRecipeId: String? = null,

    @ColumnInfo(name = "source_recipe_name")
    val sourceRecipeName: String = "",

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
