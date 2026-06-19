package com.example.mob_dev_portfolio.data.remote

import kotlinx.serialization.json.JsonElement

data class SchemaRecipeDto(
    val name: String = "",
    val recipeYield: JsonElement? = null,
    val prepTime: String = "",
    val cookTime: String = "",
    val recipeCategory: String = "",
    val recipeCuisine: String = "",
    val image: JsonElement? = null,
    val recipeIngredient: List<String> = emptyList(),
    val recipeInstructions: JsonElement? = null,
    val description: String = ""
)
