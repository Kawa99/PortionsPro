package com.example.mob_dev_portfolio.ui.favourites

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mob_dev_portfolio.R
import com.example.mob_dev_portfolio.ui.components.EmptyState
import com.example.mob_dev_portfolio.ui.components.ErrorState
import com.example.mob_dev_portfolio.ui.components.LoadingState
import com.example.mob_dev_portfolio.ui.components.RecipeCard

@Composable
fun FavouritesScreen(
    onRecipeClick: (String) -> Unit,
    onError: (String) -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { message ->
            onError(message)
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.nav_favourites),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )

            when (val state = uiState) {
                FavouritesUiState.Loading -> {
                    LoadingState(modifier = Modifier.weight(1f))
                }
                FavouritesUiState.Empty -> {
                    EmptyState(
                        iconRes = R.drawable.ic_favorite,
                        title = stringResource(R.string.favourites_empty_title),
                        message = stringResource(R.string.favourites_empty_message),
                        modifier = Modifier.weight(1f)
                    )
                }
                is FavouritesUiState.Error -> {
                    val fallbackMessage = stringResource(R.string.favourites_load_error_message)
                    ErrorState(
                        iconRes = R.drawable.ic_broken_image,
                        title = stringResource(R.string.favourites_load_error_title),
                        message = state.message.ifBlank { fallbackMessage },
                        actionLabel = stringResource(R.string.retry),
                        onActionClick = viewModel::retry,
                        modifier = Modifier.weight(1f)
                    )
                }
                is FavouritesUiState.Content -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.favourites, key = { it.id }) { recipe ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { distance -> distance * 0.4f }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                onDismiss = { direction ->
                                    if (direction == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.unfavourite(recipe.id)
                                    }
                                },
                                backgroundContent = {
                                    val backgroundColor by animateColorAsState(
                                        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            Color.Transparent
                                        },
                                        label = "favouriteDismissBackground"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(backgroundColor)
                                            .padding(end = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = stringResource(R.string.remove_from_favourites),
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            ) {
                                RecipeCard(
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
}
