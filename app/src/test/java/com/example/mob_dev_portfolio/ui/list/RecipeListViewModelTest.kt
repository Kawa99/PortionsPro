package com.example.mob_dev_portfolio.ui.list

import com.example.mob_dev_portfolio.data.RecipeSort
import com.example.mob_dev_portfolio.util.FakeRecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListViewModelTest {

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
    fun searchAreaAndSortAreSentToRepository() = runTest {
        val vm = RecipeListViewModel(fakeRepository)
        val collectionJob = launch { vm.recipes.collect {} }

        advanceTimeBy(301)
        vm.onSearchQueryChanged("dal")
        advanceTimeBy(301)
        vm.onAreaSelected("Indian")
        vm.onTimeSortPressed()
        advanceUntilIdle()

        assertEquals("dal", fakeRepository.lastRecipeQuery)
        assertEquals("Indian", fakeRepository.lastRecipeArea)
        assertEquals(RecipeSort.TimeAsc, fakeRepository.lastRecipeSort)

        collectionJob.cancel()
    }

    @Test
    fun areasUpdateReactivelyFromRepository() = runTest {
        val vm = RecipeListViewModel(fakeRepository)
        val collectionJob = launch { vm.areas.collect {} }

        fakeRepository.areasFlow.value = listOf("Indian", "Italian")
        advanceUntilIdle()

        assertEquals(listOf("Indian", "Italian"), vm.areas.value)

        collectionJob.cancel()
    }

    @Test
    fun uiStateEmitsErrorWhenRecipesFailToLoad() = runTest {
        fakeRepository.getRecipesFlowError = IllegalStateException("Database unavailable")
        val vm = RecipeListViewModel(fakeRepository)
        val state = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.first { it is RecipeListUiState.Error }
        }

        advanceTimeBy(301)
        advanceUntilIdle()

        val error = state.await() as RecipeListUiState.Error
        assertEquals("Database unavailable", error.message)
    }

    @Test
    fun sortButtonsCycleBackToNameSort() {
        val vm = RecipeListViewModel(fakeRepository)

        vm.onTimeSortPressed()
        assertEquals(RecipeSort.TimeAsc, vm.selectedSort.value)

        vm.onTimeSortPressed()
        assertEquals(RecipeSort.TimeDesc, vm.selectedSort.value)

        vm.onTimeSortPressed()
        assertEquals(RecipeSort.NameAsc, vm.selectedSort.value)
    }
}
