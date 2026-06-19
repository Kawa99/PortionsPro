package com.example.mob_dev_portfolio.data

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "recipe_summary",
    value = """
        SELECT
            r.id AS id,
            r.name AS name,
            r.category AS category,
            r.area AS area,
            r.thumbnail_url AS thumbnail_url,
            r.is_favourite AS is_favourite,
            r.is_user_created AS is_user_created,
            r.is_cached AS is_cached,
            r.base_servings AS base_servings,
            (r.prep_time_minutes + r.cook_time_minutes) AS total_time_minutes,
            (SELECT COUNT(*) FROM ingredients i WHERE i.recipe_id = r.id) AS ingredient_count
        FROM recipes r
    """
)
data class RecipeSummaryView(
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "area")
    val area: String,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String,

    @ColumnInfo(name = "is_favourite")
    val isFavourite: Boolean,

    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean,

    @ColumnInfo(name = "is_cached")
    val isCached: Boolean,

    @ColumnInfo(name = "base_servings")
    val baseServings: Int,

    @ColumnInfo(name = "total_time_minutes")
    val totalTimeMinutes: Int,

    @ColumnInfo(name = "ingredient_count")
    val ingredientCount: Int
)
