package com.example.mob_dev_portfolio.data

enum class RecipeSort(
    val sortKey: String,
    val ascending: Boolean
) {
    NameAsc("name", true),
    TimeAsc("time", true),
    TimeDesc("time", false),
    IngredientsAsc("ingredients", true),
    IngredientsDesc("ingredients", false)
}
