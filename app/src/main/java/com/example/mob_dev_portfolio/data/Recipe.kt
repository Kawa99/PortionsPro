package com.example.mob_dev_portfolio.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String = "",

    @ColumnInfo(name = "area")
    val area: String = "",

    @ColumnInfo(name = "instructions")
    val instructions: String = "",

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String = "",

    @ColumnInfo(name = "source_url")
    val sourceUrl: String = "",

    @ColumnInfo(name = "base_servings")
    val baseServings: Int = 1,

    @ColumnInfo(name = "prep_time_minutes")
    val prepTimeMinutes: Int = 0,

    @ColumnInfo(name = "cook_time_minutes")
    val cookTimeMinutes: Int = 0,

    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false,

    @ColumnInfo(name = "is_favourite")
    val isFavourite: Boolean = false,

    @ColumnInfo(name = "is_cached")
    val isCached: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
