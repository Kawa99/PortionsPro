package com.example.mob_dev_portfolio.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration_test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        createLegacyDatabase(
            version = 2,
            recipeInsertSql = """
                INSERT INTO recipes
                    (id, name, category, area, instructions,
                     thumbnailUrl, baseServings, isUserCreated, isFavourite, isCached)
                VALUES
                    ('test_id', 'Test Recipe', 'Beef', 'British', 'Cook it.',
                     '', 4, 0, 0, 1)
            """.trimIndent()
        )

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            AppDatabase.MIGRATION_2_TO_3
        )

        val cursor = db.query(
            "SELECT source_url, prep_time_minutes, cook_time_minutes, " +
                "created_at, updated_at FROM recipes WHERE id = 'test_id'"
        )
        assertTrue("Migrated row not found", cursor.moveToFirst())
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("source_url")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("prep_time_minutes")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("cook_time_minutes")))
        assertTrue(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")) > 0L)
        assertTrue(cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")) > 0L)
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To3() {
        createLegacyDatabase(
            version = 1,
            recipeInsertSql = """
                INSERT INTO recipes
                    (id, name, category, area, instructions,
                     thumbnailUrl, baseServings, isUserCreated, isFavourite, isCached)
                VALUES
                    ('test_id', 'Test Recipe', 'Chicken', 'Italian', 'Cook it.',
                     '', 2, 0, 0, 1)
            """.trimIndent()
        )

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            AppDatabase.MIGRATION_1_TO_3
        )

        val cursor = db.query("SELECT id, name FROM recipes WHERE id = 'test_id'")
        assertTrue("Row not found after 1→3 migration", cursor.moveToFirst())
        assertEquals("Test Recipe", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        cursor.close()
        db.close()
    }

    private fun createLegacyDatabase(version: Int, recipeInsertSql: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        val databaseFile = context.getDatabasePath(TEST_DB)
        databaseFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recipes (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                area TEXT NOT NULL,
                instructions TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                baseServings INTEGER NOT NULL,
                isUserCreated INTEGER NOT NULL,
                isFavourite INTEGER NOT NULL,
                isCached INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ingredients (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recipeId TEXT NOT NULL,
                name TEXT NOT NULL,
                quantity REAL NOT NULL,
                unit TEXT NOT NULL,
                FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS shopping_list (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ingredientName TEXT NOT NULL,
                quantity REAL NOT NULL,
                unit TEXT NOT NULL,
                isChecked INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ingredients_recipeId ON ingredients(recipeId)")
        db.execSQL(recipeInsertSql)
        db.version = version
        db.close()
    }
}
