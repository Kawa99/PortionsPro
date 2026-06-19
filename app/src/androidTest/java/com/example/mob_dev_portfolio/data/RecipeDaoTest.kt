package com.example.mob_dev_portfolio.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun setUp() {
        // inMemoryDatabaseBuilder creates a fresh database for every test.
        // allowMainThreadQueries() is acceptable in tests only.
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.recipeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertedRecipeIsReturnedByGetAllSummaries() = runTest {
        dao.insertRecipe(testRecipe("r1", "Carbonara"))

        val result = dao.getAllSummaries().first()

        assertEquals(1, result.size)
        assertEquals("r1", result[0].id)
        assertEquals("Carbonara", result[0].name)
    }

    @Test
    fun deletingRecipeCascadesToIngredients() = runTest {
        dao.insertRecipe(testRecipe("r1", "Carbonara"))
        dao.insertIngredients(
            listOf(
                testIngredient("r1", "Spaghetti", displayOrder = 0),
                testIngredient("r1", "Guanciale", displayOrder = 1)
            )
        )

        val before = dao.getRecipeWithIngredients("r1").first()
        assertNotNull(before)
        assertEquals(2, before!!.ingredients.size)

        dao.deleteRecipe(testRecipe("r1", "Carbonara"))

        val after = dao.getRecipeWithIngredients("r1").first()
        assertNull(after)
    }

    @Test
    fun setFavouriteUpdatesOnlyFavouriteFlag() = runTest {
        dao.insertRecipe(testRecipe("r1", "Carbonara", isFavourite = false))

        dao.setFavourite("r1", true)

        val favourites = dao.getFavourites().first()
        assertEquals(1, favourites.size)
        assertEquals("r1", favourites[0].id)
        assertEquals("Carbonara", favourites[0].name)
        assertTrue(favourites[0].isFavourite)
    }

    @Test
    fun searchSummariesReturnsOnlyMatchingNames() = runTest {
        dao.insertRecipes(
            listOf(
                testRecipe("r1", "Spaghetti Carbonara"),
                testRecipe("r2", "Chicken Tikka"),
                testRecipe("r3", "Spaghetti Bolognese")
            )
        )

        val result = dao.searchSummaries("Spaghetti").first()

        assertEquals(2, result.size)
        assertTrue(result.all { "Spaghetti" in it.name })
        assertTrue(result.none { it.name == "Chicken Tikka" })
    }

    @Test
    fun ingredientsAreReturnedInDisplayOrderSequence() = runTest {
        dao.insertRecipe(testRecipe("r1", "Test Recipe"))
        dao.insertIngredients(
            listOf(
                testIngredient("r1", "C", displayOrder = 2),
                testIngredient("r1", "A", displayOrder = 0),
                testIngredient("r1", "B", displayOrder = 1)
            )
        )

        val result = dao.getRecipeWithIngredients("r1").first()
        assertNotNull(result)

        val sortedNames = result!!.ingredients.sortedBy { it.displayOrder }.map { it.name }
        assertEquals(listOf("A", "B", "C"), sortedNames)
    }

    @Test
    fun recipeSummaryViewReturnsCorrectIngredientCount() = runTest {
        dao.insertRecipe(testRecipe("r1", "Test Recipe"))
        dao.insertIngredients(
            listOf(
                testIngredient("r1", "Flour", displayOrder = 0),
                testIngredient("r1", "Eggs", displayOrder = 1),
                testIngredient("r1", "Butter", displayOrder = 2)
            )
        )

        val summaries = dao.getAllSummaries().first()

        assertEquals(1, summaries.size)
        assertEquals(3, summaries[0].ingredientCount)
    }

    @Test
    fun getSummariesSortsByTotalTimeAscending() = runTest {
        dao.insertRecipes(
            listOf(
                testRecipe("r1", "Quick Pasta", prepTimeMinutes = 5, cookTimeMinutes = 15),
                testRecipe("r2", "Slow Soup", prepTimeMinutes = 15, cookTimeMinutes = 60),
                testRecipe("r3", "Medium Curry", prepTimeMinutes = 10, cookTimeMinutes = 35)
            )
        )

        val result = dao.getSummaries(
            query = "",
            area = "",
            sortKey = "time",
            ascending = true
        ).first()

        assertEquals(listOf("Quick Pasta", "Medium Curry", "Slow Soup"), result.map { it.name })
    }

    @Test
    fun getSummariesSortsByTotalTimeDescending() = runTest {
        dao.insertRecipes(
            listOf(
                testRecipe("r1", "Quick Pasta", prepTimeMinutes = 5, cookTimeMinutes = 15),
                testRecipe("r2", "Slow Soup", prepTimeMinutes = 15, cookTimeMinutes = 60),
                testRecipe("r3", "Medium Curry", prepTimeMinutes = 10, cookTimeMinutes = 35)
            )
        )

        val result = dao.getSummaries(
            query = "",
            area = "",
            sortKey = "time",
            ascending = false
        ).first()

        assertEquals(listOf("Slow Soup", "Medium Curry", "Quick Pasta"), result.map { it.name })
    }

    @Test
    fun getSummariesSortsByIngredientCountDescending() = runTest {
        dao.insertRecipes(
            listOf(
                testRecipe("r1", "Simple Toast"),
                testRecipe("r2", "Loaded Curry")
            )
        )
        dao.insertIngredients(
            listOf(
                testIngredient("r1", "Bread", displayOrder = 0),
                testIngredient("r1", "Butter", displayOrder = 1),
                testIngredient("r2", "Onion", displayOrder = 0),
                testIngredient("r2", "Garlic", displayOrder = 1),
                testIngredient("r2", "Tomatoes", displayOrder = 2),
                testIngredient("r2", "Chickpeas", displayOrder = 3),
                testIngredient("r2", "Spinach", displayOrder = 4),
                testIngredient("r2", "Rice", displayOrder = 5)
            )
        )

        val result = dao.getSummaries(
            query = "",
            area = "",
            sortKey = "ingredients",
            ascending = false
        ).first()

        assertEquals(listOf("Loaded Curry", "Simple Toast"), result.map { it.name })
    }

    @Test
    fun getSummariesSortsByIngredientCountAscending() = runTest {
        dao.insertRecipes(
            listOf(
                testRecipe("r1", "Simple Toast"),
                testRecipe("r2", "Loaded Curry")
            )
        )
        dao.insertIngredients(
            listOf(
                testIngredient("r1", "Bread", displayOrder = 0),
                testIngredient("r1", "Butter", displayOrder = 1),
                testIngredient("r2", "Onion", displayOrder = 0),
                testIngredient("r2", "Garlic", displayOrder = 1),
                testIngredient("r2", "Tomatoes", displayOrder = 2),
                testIngredient("r2", "Chickpeas", displayOrder = 3),
                testIngredient("r2", "Spinach", displayOrder = 4),
                testIngredient("r2", "Rice", displayOrder = 5)
            )
        )

        val result = dao.getSummaries(
            query = "",
            area = "",
            sortKey = "ingredients",
            ascending = true
        ).first()

        assertEquals(listOf("Simple Toast", "Loaded Curry"), result.map { it.name })
    }

    @Test
    fun getSummariesFiltersByArea() = runTest {
        dao.insertRecipes(
            listOf(
                testRecipe("r1", "Carbonara", area = "Italian"),
                testRecipe("r2", "Chicken Tikka", area = "Indian"),
                testRecipe("r3", "Dal", area = "Indian")
            )
        )

        val result = dao.getSummaries(
            query = "",
            area = "Indian",
            sortKey = "name",
            ascending = true
        ).first()

        assertEquals(listOf("Chicken Tikka", "Dal"), result.map { it.name })
    }

    @Test
    fun getDistinctAreasFlowReturnsAreasNotCategories() = runTest {
        dao.insertRecipes(
            listOf(
                testRecipe("r1", "Carbonara", category = "Pasta", area = "Italian"),
                testRecipe("r2", "Dal", category = "Vegetarian", area = "Indian"),
                testRecipe("r3", "Curry", category = "Chicken", area = "Indian")
            )
        )

        assertEquals(listOf("Indian", "Italian"), dao.getDistinctAreasFlow().first())
    }

    private fun testRecipe(
        id: String,
        name: String,
        category: String = "TestCategory",
        area: String = "TestArea",
        isFavourite: Boolean = false,
        prepTimeMinutes: Int = 0,
        cookTimeMinutes: Int = 0
    ) = Recipe(
        id = id,
        name = name,
        category = category,
        area = area,
        instructions = "Test instructions",
        thumbnailUrl = "",
        sourceUrl = "",
        baseServings = 4,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        isFavourite = isFavourite
    )

    private fun testIngredient(
        recipeId: String,
        name: String,
        quantity: Double = 100.0,
        unit: String = "g",
        displayOrder: Int = 0
    ) = Ingredient(
        recipeId = recipeId,
        name = name,
        quantity = quantity,
        unit = unit,
        displayOrder = displayOrder
    )
}
