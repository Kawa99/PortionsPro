package com.example.mob_dev_portfolio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Schema JSON identity hash: 6b4693d5d80d6565af765be744f46993
// Asset DB identity hash:   6b4693d5d80d6565af765be744f46993
// MATCH - hashes are identical. No action needed.
@Database(
    entities = [Recipe::class, Ingredient::class, ShoppingListItem::class],
    views = [RecipeSummaryView::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun shoppingListDao(): ShoppingListDao

    companion object {
        private fun migrateLegacySchema(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recipes_new (
                    id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    area TEXT NOT NULL,
                    instructions TEXT NOT NULL,
                    thumbnail_url TEXT NOT NULL,
                    source_url TEXT NOT NULL,
                    base_servings INTEGER NOT NULL,
                    prep_time_minutes INTEGER NOT NULL,
                    cook_time_minutes INTEGER NOT NULL,
                    is_user_created INTEGER NOT NULL,
                    is_favourite INTEGER NOT NULL,
                    is_cached INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO recipes_new (
                    id,
                    name,
                    category,
                    area,
                    instructions,
                    thumbnail_url,
                    source_url,
                    base_servings,
                    prep_time_minutes,
                    cook_time_minutes,
                    is_user_created,
                    is_favourite,
                    is_cached,
                    created_at,
                    updated_at
                )
                SELECT
                    id,
                    name,
                    category,
                    area,
                    instructions,
                    thumbnailUrl,
                    '',
                    baseServings,
                    0,
                    0,
                    isUserCreated,
                    isFavourite,
                    isCached,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    CAST(strftime('%s','now') AS INTEGER) * 1000
                FROM recipes
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ingredients_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    recipe_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    display_order INTEGER NOT NULL,
                    FOREIGN KEY(recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO ingredients_new (
                    id,
                    recipe_id,
                    name,
                    quantity,
                    unit,
                    display_order
                )
                SELECT
                    id,
                    recipeId,
                    name,
                    quantity,
                    unit,
                    0
                FROM ingredients
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS shopping_list_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ingredient_name TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    is_checked INTEGER NOT NULL,
                    source_recipe_id TEXT,
                    source_recipe_name TEXT NOT NULL,
                    added_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO shopping_list_new (
                    id,
                    ingredient_name,
                    quantity,
                    unit,
                    is_checked,
                    source_recipe_id,
                    source_recipe_name,
                    added_at
                )
                SELECT
                    id,
                    ingredientName,
                    quantity,
                    unit,
                    isChecked,
                    NULL,
                    '',
                    CAST(strftime('%s','now') AS INTEGER) * 1000
                FROM shopping_list
                """.trimIndent()
            )

            db.execSQL("DROP TABLE ingredients")
            db.execSQL("DROP TABLE shopping_list")
            db.execSQL("DROP TABLE recipes")
            db.execSQL("ALTER TABLE recipes_new RENAME TO recipes")
            db.execSQL("ALTER TABLE ingredients_new RENAME TO ingredients")
            db.execSQL("ALTER TABLE shopping_list_new RENAME TO shopping_list")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ingredients_recipe_id ON ingredients(recipe_id)")
            db.execSQL("DROP VIEW IF EXISTS recipe_summary")
            db.execSQL(
                """
                CREATE VIEW `recipe_summary` AS SELECT
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
                """.trimIndent()
            )
        }

        internal val MIGRATION_0_TO_3 = object : Migration(0, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateLegacySchema(db)
            }
        }

        internal val MIGRATION_1_TO_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateLegacySchema(db)
            }
        }

        internal val MIGRATION_2_TO_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateLegacySchema(db)
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "portions_pro_db"
                )
                    .createFromAsset("database/portions_pro.db")
                    .addMigrations(
                        MIGRATION_0_TO_3,
                        MIGRATION_1_TO_3,
                        MIGRATION_2_TO_3
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
