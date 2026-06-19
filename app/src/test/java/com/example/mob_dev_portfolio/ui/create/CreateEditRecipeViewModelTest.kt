package com.example.mob_dev_portfolio.ui.create

import android.net.Uri
import com.example.mob_dev_portfolio.data.Ingredient
import com.example.mob_dev_portfolio.data.Recipe
import com.example.mob_dev_portfolio.data.RecipeImportDraft
import com.example.mob_dev_portfolio.data.RecipeWithIngredients
import com.example.mob_dev_portfolio.util.FakeRecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditRecipeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRecipeRepository
    private lateinit var fakeImageStorage: FakeRecipeImageStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRecipeRepository()
        fakeImageStorage = FakeRecipeImageStorage()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun formEditingIsOwnedByViewModelAndSaveInsertsRecipe() = runTest {
        val vm = createViewModel()

        vm.onNameChanged("Pancakes")
        vm.onCategoryChanged("Breakfast")
        vm.onBaseServingsChanged("3")
        vm.onIngredientNameChanged(0, "Flour")
        vm.onIngredientQuantityChanged(0, "250")
        vm.onIngredientUnitChanged(0, "g")
        vm.onInstructionChanged(0, "Mix")
        val errors = vm.saveRecipe("NEW")
        advanceUntilIdle()

        assertFalse(errors.hasErrors)
        assertEquals("Pancakes", fakeRepository.insertedRecipe?.name)
        assertEquals("Breakfast", fakeRepository.insertedRecipe?.category)
        assertEquals(3, fakeRepository.insertedRecipe?.baseServings)
        assertEquals("Mix", fakeRepository.insertedRecipe?.instructions)
        assertEquals(1, fakeRepository.insertedIngredients.size)
        assertEquals("Flour", fakeRepository.insertedIngredients[0].name)
        assertTrue(vm.uiState.value is CreateEditUiState.Saved)
    }

    @Test
    fun saveStoresValidationErrorsInViewModelState() = runTest {
        val vm = createViewModel()

        vm.onBaseServingsChanged("0")
        vm.onIngredientNameChanged(0, "Flour")
        vm.onIngredientQuantityChanged(0, "bad")
        val errors = vm.saveRecipe("NEW")

        assertTrue(errors.name)
        assertTrue(errors.servings)
        assertEquals(setOf(0), errors.ingredientQuantityErrorIndexes)
        assertEquals(errors, vm.formErrors.value)
        assertEquals(null, fakeRepository.insertedRecipe)
    }

    @Test
    fun loadRecipeMapsRepositoryRecipeIntoFormState() = runTest {
        val vm = createViewModel()
        fakeRepository.recipeFlow.value = RecipeWithIngredients(
            recipe = Recipe(
                id = "recipe-1",
                name = "Carbonara",
                category = "Dinner",
                area = "Italian",
                instructions = "Boil pasta\nMix sauce",
                thumbnailUrl = "file://image.jpg",
                sourceUrl = "https://example.com/carbonara",
                baseServings = 2,
                prepTimeMinutes = 5,
                cookTimeMinutes = 15
            ),
            ingredients = listOf(
                Ingredient(recipeId = "recipe-1", name = "Pasta", quantity = 200.0, unit = "g", displayOrder = 0)
            )
        )

        vm.loadRecipe("recipe-1")
        advanceUntilIdle()

        val form = vm.formState.value
        assertEquals("Carbonara", form.name)
        assertEquals("Italian", form.area)
        assertEquals("2", form.baseServings)
        assertEquals("Boil pasta", form.instructionRows[0].text)
        assertEquals("Pasta", form.ingredientRows[0].name)
        assertEquals("200", form.ingredientRows[0].quantity)
    }

    @Test
    fun loadImportDraftClearsInsecureRemoteThumbnail() = runTest {
        fakeRepository.importDraft = RecipeImportDraft(
            recipe = Recipe(
                id = "draft-recipe",
                name = "Imported",
                thumbnailUrl = "http://example.com/image.jpg"
            ),
            ingredients = emptyList()
        )
        val vm = createViewModel()

        vm.loadImportDraft("draft-1")
        advanceUntilIdle()

        assertEquals("Imported", vm.formState.value.name)
        assertEquals("", vm.formState.value.thumbnailUrl)
    }

    private fun createViewModel(): CreateEditRecipeViewModel {
        return CreateEditRecipeViewModel(
            repository = fakeRepository,
            imageStorage = fakeImageStorage
        )
    }

    private class FakeRecipeImageStorage : RecipeImageStorageInterface {
        override suspend fun copyToInternalStorage(sourceUri: Uri): String {
            return "file://copied-image.jpg"
        }

        override suspend fun downloadToInternalStorage(imageUrl: String): String {
            return "file://downloaded-image.jpg"
        }
    }
}
