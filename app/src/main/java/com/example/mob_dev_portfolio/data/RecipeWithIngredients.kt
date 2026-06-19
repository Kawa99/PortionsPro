package com.example.mob_dev_portfolio.data

import androidx.room.Embedded
import androidx.room.Relation

data class RecipeWithIngredients(
    @Embedded
    val recipe: Recipe,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id"
    )
    val ingredients: List<Ingredient>
)
