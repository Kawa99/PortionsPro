package com.example.mob_dev_portfolio.ui.detail

import com.example.mob_dev_portfolio.data.Ingredient
import com.example.mob_dev_portfolio.data.Recipe
import com.example.mob_dev_portfolio.data.RecipeWithIngredients
import com.example.mob_dev_portfolio.util.FakeRecipeRepository
import com.example.mob_dev_portfolio.util.FakeSettingsRepository
import com.example.mob_dev_portfolio.util.savedStateHandleOf
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRecipeRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRecipeRepository()
        fakeSettingsRepository = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scalingFromBase4To8DoublesEveryQuantity() = runTest {
        val vm = createViewModel()
        fakeRepository.recipeFlow.value = recipeWithIngredients(baseServings = 4)
        advanceUntilIdle()

        vm.setPortions(8)
        advanceUntilIdle()

        val state = vm.uiState.value as RecipeDetailUiState.Success
        assertEquals(8, state.portions)

        val flour = state.scaledIngredients.first { it.name == "Flour" }
        assertEquals(800.0, flour.scaledQuantity, 0.001)
        assertEquals("g", flour.unit)

        val eggs = state.scaledIngredients.first { it.name == "Eggs" }
        assertEquals(8.0, eggs.scaledQuantity, 0.001)
        assertEquals("", eggs.unit)
    }

    @Test
    fun imperialSettingConvertsGramsToOunces() = runTest {
        fakeSettingsRepository.setUseMetric(false)
        val vm = createViewModel()
        fakeRepository.recipeFlow.value = recipeWithIngredients(baseServings = 4)
        advanceUntilIdle()

        val state = vm.uiState.value as RecipeDetailUiState.Success
        val flour = state.scaledIngredients.first { it.name == "Flour" }

        assertEquals("oz", flour.unit)
        assertEquals(14.11, flour.scaledQuantity, 0.01)
    }

    @Test
    fun uiStateIsLoadingBeforeRepositoryEmits() {
        val vm = createViewModel()
        assertTrue(vm.uiState.value is RecipeDetailUiState.Loading)
    }

    @Test
    fun successStateContainsParsedInstructionSteps() = runTest {
        val vm = createViewModel()
        fakeRepository.recipeFlow.value = recipeWithIngredients(
            baseServings = 4,
            instructions = " Mix batter \n\n Cook pancakes "
        )
        advanceUntilIdle()

        val state = vm.uiState.value as RecipeDetailUiState.Success

        assertEquals(listOf("Mix batter", "Cook pancakes"), state.instructionSteps.map { it.text })
        assertEquals(listOf("0-Mix batter", "1-Cook pancakes"), state.instructionSteps.map { it.id })
    }

    @Test
    fun addToShoppingListUsesAlreadyConvertedQuantities() = runTest {
        fakeSettingsRepository.setUseMetric(false)
        val vm = createViewModel()
        fakeRepository.recipeFlow.value = recipeWithIngredients(baseServings = 4)
        advanceUntilIdle()
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.first()
        }

        vm.addToShoppingList()
        advanceUntilIdle()

        val items = fakeRepository.lastShoppingListItems
        assertTrue(items.isNotEmpty())

        val flour = items.first { it.ingredientName == "Flour" }
        assertEquals("oz", flour.unit)
        assertTrue(flour.quantity < 20.0)
        assertTrue(event.await() is RecipeDetailEvent.ShoppingListAddSucceeded)
    }

    @Test
    fun addToShoppingListEmitsFailureEventWhenRepositoryThrows() = runTest {
        val vm = createViewModel()
        fakeRepository.recipeFlow.value = recipeWithIngredients(baseServings = 4)
        fakeRepository.addShoppingListItemsError = IllegalStateException("Disk full")
        advanceUntilIdle()
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.first()
        }

        vm.addToShoppingList()
        advanceUntilIdle()

        val failure = event.await() as RecipeDetailEvent.ShoppingListAddFailed
        assertEquals("Disk full", failure.message)
    }

    @Test
    fun toggleFavouriteEmitsFailureEventWhenRepositoryThrows() = runTest {
        val vm = createViewModel()
        fakeRepository.recipeFlow.value = recipeWithIngredients(baseServings = 4)
        fakeRepository.setFavouriteError = IllegalStateException("Favourite failed")
        advanceUntilIdle()
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.first()
        }

        vm.toggleFavourite()
        advanceUntilIdle()

        val failure = event.await() as RecipeDetailEvent.FavouriteUpdateFailed
        assertEquals("Favourite failed", failure.message)
    }

    private fun createViewModel(recipeId: String = "test_id"): RecipeDetailViewModel {
        return RecipeDetailViewModel(
            savedStateHandle = savedStateHandleOf("recipeId" to recipeId),
            repository = fakeRepository,
            settingsRepository = fakeSettingsRepository
        )
    }

    private fun recipeWithIngredients(
        id: String = "test_id",
        name: String = "Test Recipe",
        baseServings: Int = 4,
        instructions: String = "",
        ingredients: List<Ingredient> = listOf(
            Ingredient(recipeId = id, name = "Flour", quantity = 400.0, unit = "g", displayOrder = 0),
            Ingredient(recipeId = id, name = "Butter", quantity = 200.0, unit = "g", displayOrder = 1),
            Ingredient(recipeId = id, name = "Eggs", quantity = 4.0, unit = "", displayOrder = 2)
        )
    ) = RecipeWithIngredients(
        recipe = Recipe(id = id, name = name, baseServings = baseServings, instructions = instructions),
        ingredients = ingredients
    )
}
