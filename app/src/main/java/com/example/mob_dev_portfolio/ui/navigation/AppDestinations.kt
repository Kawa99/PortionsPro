package com.example.mob_dev_portfolio.ui.navigation

import com.example.mob_dev_portfolio.R
import kotlinx.serialization.Serializable

// Type-safe routes. @Serializable enables Navigation Compose to encode/decode
// arguments without string templates. No Safe Args plugin required.
@Serializable
object Onboarding

@Serializable
object RecipeList

@Serializable
object Favourites

@Serializable
object ShoppingList

@Serializable
object Settings

@Serializable
object ImportRecipe

// Routes with arguments use data classes.
@Serializable
data class RecipeDetail(val recipeId: String)

@Serializable
data class CreateEditRecipe(
    val recipeId: String = "NEW",
    val importDraftId: String? = null
)

// The bottom nav tabs as a list for iteration.
sealed class BottomTab(
    val route: Any,
    val labelRes: Int,
    val iconRes: Int,
    val iconContentDesc: Int,
) {
    data object Recipes : BottomTab(RecipeList, R.string.nav_recipes, R.drawable.ic_restaurant, R.string.nav_recipes)
    data object Favs : BottomTab(Favourites, R.string.nav_favourites, R.drawable.ic_favorite, R.string.nav_favourites)
    data object Create : BottomTab(CreateEditRecipe(), R.string.nav_create, R.drawable.ic_add_circle, R.string.nav_create)
    data object Shopping : BottomTab(ShoppingList, R.string.nav_shopping, R.drawable.ic_shopping_cart, R.string.nav_shopping)
    data object SettingsT : BottomTab(Settings, R.string.nav_settings, R.drawable.ic_settings, R.string.nav_settings)
}

val bottomTabs = listOf(
    BottomTab.Recipes,
    BottomTab.Favs,
    BottomTab.Create,
    BottomTab.Shopping,
    BottomTab.SettingsT,
)
