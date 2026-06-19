package com.example.mob_dev_portfolio.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipe_id"])]
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "recipe_id")
    val recipeId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "quantity")
    val quantity: Double,

    @ColumnInfo(name = "unit")
    val unit: String = "",

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0
)
