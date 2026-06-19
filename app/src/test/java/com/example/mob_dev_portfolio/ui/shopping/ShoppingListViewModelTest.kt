package com.example.mob_dev_portfolio.ui.shopping

import com.example.mob_dev_portfolio.data.ShoppingListItem
import com.example.mob_dev_portfolio.util.FakeRecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
class ShoppingListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRecipeRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRecipeRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleCheckedPersistsOppositeCheckedState() = runTest {
        val vm = ShoppingListViewModel(fakeRepository)
        val item = ShoppingListItem(
            id = 7,
            ingredientName = "Flour",
            quantity = 250.0,
            unit = "g",
            isChecked = false
        )

        vm.toggleChecked(item)
        advanceUntilIdle()

        assertEquals(item.copy(isChecked = true), fakeRepository.updatedShoppingItem)
    }

    @Test
    fun clearActionsDelegateToRepository() = runTest {
        val vm = ShoppingListViewModel(fakeRepository)

        vm.clearChecked()
        vm.clearAll()
        advanceUntilIdle()

        assertTrue(fakeRepository.didClearCheckedShoppingItems)
        assertTrue(fakeRepository.didClearShoppingList)
    }

    @Test
    fun sectionsGroupItemsBySourceRecipeName() = runTest {
        val vm = ShoppingListViewModel(fakeRepository)
        val sectionsResult = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.sections.first { it.isNotEmpty() }
        }

        fakeRepository.shoppingItemsFlow.value = listOf(
            ShoppingListItem(
                id = 1,
                ingredientName = "Flour",
                quantity = 250.0,
                sourceRecipeName = "Pancakes"
            ),
            ShoppingListItem(
                id = 2,
                ingredientName = "Eggs",
                quantity = 2.0,
                sourceRecipeName = "Pancakes"
            ),
            ShoppingListItem(
                id = 3,
                ingredientName = "Salt",
                quantity = 1.0,
                sourceRecipeName = ""
            )
        )
        advanceUntilIdle()

        val sections = sectionsResult.await()
        assertEquals(2, sections.size)
        assertEquals("Pancakes", sections[0].sourceName)
        assertEquals(listOf("Flour", "Eggs"), sections[0].items.map { it.ingredientName })
        assertEquals(ShoppingListViewModel.OTHER_ITEMS_SOURCE_NAME, sections[1].sourceName)
        assertEquals(listOf("Salt"), sections[1].items.map { it.ingredientName })
    }

    @Test
    fun uiStateEmitsErrorWhenShoppingListFailsToLoad() = runTest {
        fakeRepository.getShoppingListFlowError = IllegalStateException("Read failed")
        val vm = ShoppingListViewModel(fakeRepository)
        val state = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.first { it is ShoppingListUiState.Error }
        }

        advanceUntilIdle()

        val error = state.await() as ShoppingListUiState.Error
        assertEquals("Read failed", error.message)
    }

    @Test
    fun toggleCheckedEmitsErrorWhenRepositoryThrows() = runTest {
        fakeRepository.updateShoppingItemError = IllegalStateException("Update failed")
        val vm = ShoppingListViewModel(fakeRepository)
        val item = ShoppingListItem(
            id = 7,
            ingredientName = "Flour",
            quantity = 250.0,
            unit = "g",
            isChecked = false
        )
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.errorEvents.first()
        }

        vm.toggleChecked(item)
        advanceUntilIdle()

        assertEquals("Update failed", event.await())
    }

    @Test
    fun clearAllEmitsErrorWhenRepositoryThrows() = runTest {
        fakeRepository.clearShoppingListError = IllegalStateException("Clear failed")
        val vm = ShoppingListViewModel(fakeRepository)
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.errorEvents.first()
        }

        vm.clearAll()
        advanceUntilIdle()

        assertEquals("Clear failed", event.await())
    }
}
