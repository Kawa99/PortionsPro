package com.example.mob_dev_portfolio.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mob_dev_portfolio.R
import com.example.mob_dev_portfolio.data.RecipeSort
import com.example.mob_dev_portfolio.ui.components.EmptyState
import com.example.mob_dev_portfolio.ui.components.ErrorState
import com.example.mob_dev_portfolio.ui.components.LoadingState
import com.example.mob_dev_portfolio.ui.components.RecipeGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedArea by viewModel.selectedArea.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val recipeGridState = rememberLazyGridState()

    LaunchedEffect(searchQuery, selectedArea, selectedSort) {
        recipeGridState.scrollToItem(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_circle),
                    contentDescription = stringResource(R.string.nav_create)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.nav_recipes),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.search_recipes_hint)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )

            FilterHeader(
                expanded = filtersExpanded,
                selectedArea = selectedArea,
                selectedSort = selectedSort,
                onToggle = { filtersExpanded = !filtersExpanded }
            )

            if (filtersExpanded) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedArea.isEmpty(),
                            onClick = { viewModel.onAreaSelected("") },
                            label = { Text(stringResource(R.string.category_all_label)) }
                        )
                    }
                    items(areas) { area ->
                        FilterChip(
                            selected = selectedArea == area,
                            onClick = { viewModel.onAreaSelected(area) },
                            label = { Text(area) }
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSort == RecipeSort.TimeAsc || selectedSort == RecipeSort.TimeDesc,
                            onClick = viewModel::onTimeSortPressed,
                            label = { Text(timeSortLabel(selectedSort)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSort == RecipeSort.IngredientsAsc || selectedSort == RecipeSort.IngredientsDesc,
                            onClick = viewModel::onIngredientSortPressed,
                            label = { Text(ingredientSortLabel(selectedSort)) }
                        )
                    }
                }
            }

            when (val state = uiState) {
                RecipeListUiState.Loading -> {
                    LoadingState(modifier = Modifier.weight(1f))
                }
                RecipeListUiState.Empty -> {
                    EmptyState(
                        iconRes = R.drawable.ic_search,
                        title = stringResource(R.string.empty_recipe_list_title),
                        message = stringResource(R.string.empty_recipe_list_message),
                        modifier = Modifier.weight(1f)
                    )
                }
                is RecipeListUiState.Error -> {
                    val fallbackMessage = stringResource(R.string.recipes_load_error_message)
                    ErrorState(
                        iconRes = R.drawable.ic_broken_image,
                        title = stringResource(R.string.recipes_load_error_title),
                        message = state.message.ifBlank { fallbackMessage },
                        actionLabel = stringResource(R.string.retry),
                        onActionClick = viewModel::retry,
                        modifier = Modifier.weight(1f)
                    )
                }
                is RecipeListUiState.Content -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        state = recipeGridState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        gridItems(
                            items = state.recipes,
                            key = { it.id }
                        ) { recipe ->
                            RecipeGridCard(
                                recipe = recipe,
                                onClick = { onRecipeClick(recipe.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterHeader(
    expanded: Boolean,
    selectedArea: String,
    selectedSort: RecipeSort,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onToggle,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = null
                )
            },
            label = { Text(filterSummary(selectedArea, selectedSort)) }
        )
        IconButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                ),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun filterSummary(selectedArea: String, selectedSort: RecipeSort): String {
    val area = selectedArea.ifEmpty { stringResource(R.string.recipe_filter_all_areas) }
    val sort = when (selectedSort) {
        RecipeSort.NameAsc -> stringResource(R.string.recipe_sort_name_asc_short)
        RecipeSort.TimeAsc -> stringResource(R.string.recipe_sort_time_asc_short)
        RecipeSort.TimeDesc -> stringResource(R.string.recipe_sort_time_desc_short)
        RecipeSort.IngredientsAsc -> stringResource(R.string.recipe_sort_ingredients_asc_short)
        RecipeSort.IngredientsDesc -> stringResource(R.string.recipe_sort_ingredients_desc_short)
    }
    return stringResource(R.string.recipe_filter_summary, area, sort)
}

@Composable
private fun timeSortLabel(selectedSort: RecipeSort): String {
    return when (selectedSort) {
        RecipeSort.TimeAsc -> stringResource(R.string.recipe_sort_time_asc)
        RecipeSort.TimeDesc -> stringResource(R.string.recipe_sort_time_desc)
        else -> stringResource(R.string.recipe_sort_time)
    }
}

@Composable
private fun ingredientSortLabel(selectedSort: RecipeSort): String {
    return when (selectedSort) {
        RecipeSort.IngredientsAsc -> stringResource(R.string.recipe_sort_ingredients_asc)
        RecipeSort.IngredientsDesc -> stringResource(R.string.recipe_sort_ingredients_desc)
        else -> stringResource(R.string.recipe_sort_ingredients)
    }
}
