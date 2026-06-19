package com.example.mob_dev_portfolio.ui.favourites

import com.example.mob_dev_portfolio.util.FakeRecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModelTest {

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
    fun unfavouriteClearsFavouriteFlagInRepository() = runTest {
        val vm = FavouritesViewModel(fakeRepository)

        vm.unfavourite("recipe-1")
        advanceUntilIdle()

        assertEquals("recipe-1" to false, fakeRepository.lastFavouriteSet)
    }

    @Test
    fun unfavouriteEmitsErrorWhenRepositoryThrows() = runTest {
        fakeRepository.setFavouriteError = IllegalStateException("Write failed")
        val vm = FavouritesViewModel(fakeRepository)
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.errorEvents.first()
        }

        vm.unfavourite("recipe-1")
        advanceUntilIdle()

        assertEquals("Write failed", event.await())
    }

    @Test
    fun uiStateEmitsErrorWhenFavouritesFailToLoad() = runTest {
        fakeRepository.getFavouritesFlowError = IllegalStateException("Read failed")
        val vm = FavouritesViewModel(fakeRepository)
        val state = async(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.first { it is FavouritesUiState.Error }
        }

        advanceUntilIdle()

        val error = state.await() as FavouritesUiState.Error
        assertEquals("Read failed", error.message)
    }
}
