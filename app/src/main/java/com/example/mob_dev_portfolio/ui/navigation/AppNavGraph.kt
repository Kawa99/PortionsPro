package com.example.mob_dev_portfolio.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mob_dev_portfolio.R
import com.example.mob_dev_portfolio.ui.create.CreateEditRecipeScreen
import com.example.mob_dev_portfolio.ui.detail.RecipeDetailScreen
import com.example.mob_dev_portfolio.ui.favourites.FavouritesScreen
import com.example.mob_dev_portfolio.ui.import_recipe.ImportRecipeScreen
import com.example.mob_dev_portfolio.ui.list.RecipeListScreen
import com.example.mob_dev_portfolio.ui.onboarding.OnboardingScreen
import com.example.mob_dev_portfolio.ui.settings.SettingsScreen
import com.example.mob_dev_portfolio.ui.shopping.ShoppingListScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = RecipeList,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val shoppingListAddedMessage = stringResource(R.string.shopping_list_added)
    val viewActionLabel = stringResource(R.string.view)
    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }
    val showBottomBar = remember(currentDestination) {
        bottomTabs.any { tab ->
            currentDestination?.hasRoute(tab.route::class) == true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hasRoute(tab.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo<RecipeList> {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = stringResource(tab.iconContentDesc)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Onboarding> {
                OnboardingScreen(
                    onError = showSnackbar,
                    onFinished = {
                        navController.navigate(RecipeList) {
                            popUpTo<Onboarding> { inclusive = true }
                        }
                    }
                )
            }
            composable<RecipeList> {
                RecipeListScreen(
                    onRecipeClick = { recipeId ->
                        navController.navigate(RecipeDetail(recipeId))
                    },
                    onCreateClick = {
                        navController.navigate(CreateEditRecipe()) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable<RecipeDetail> {
                RecipeDetailScreen(
                    onNavigateUp = { navController.navigateUp() },
                    onItemsAddedToShopping = {
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = shoppingListAddedMessage,
                                actionLabel = viewActionLabel,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                navController.navigate(ShoppingList) {
                                    popUpTo<RecipeList> {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                    onError = showSnackbar,
                    onEditClick = { recipeId ->
                        navController.navigate(CreateEditRecipe(recipeId))
                    }
                )
            }
            composable<Favourites> {
                FavouritesScreen(
                    onRecipeClick = { recipeId ->
                        navController.navigate(RecipeDetail(recipeId))
                    },
                    onError = showSnackbar
                )
            }
            composable<ShoppingList> {
                ShoppingListScreen(onError = showSnackbar)
            }
            composable<Settings> {
                SettingsScreen(onError = showSnackbar)
            }
            composable<ImportRecipe> {
                ImportRecipeScreen(
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToDraft = { draftId ->
                        navController.navigate(CreateEditRecipe(importDraftId = draftId)) {
                            popUpTo<ImportRecipe> { inclusive = true }
                        }
                    }
                )
            }
            composable<CreateEditRecipe> { backStackEntry ->
                val route = backStackEntry.toRoute<CreateEditRecipe>()
                CreateEditRecipeScreen(
                    onCancel = { navController.navigateUp() },
                    onSaved = { recipeId ->
                        navController.navigate(RecipeDetail(recipeId)) {
                            popUpTo<RecipeList> { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onDeleted = {
                        navController.navigate(RecipeList) {
                            popUpTo<RecipeList> { inclusive = false }
                        }
                    },
                    onNavigateToImport = {
                        navController.navigate(ImportRecipe)
                    },
                    recipeId = route.recipeId,
                    importDraftId = route.importDraftId
                )
            }
        }
    }
}
