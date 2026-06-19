package com.example.mob_dev_portfolio.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.mob_dev_portfolio.R
import com.example.mob_dev_portfolio.ui.components.InstructionStep
import com.example.mob_dev_portfolio.util.CanonicalUnit
import com.example.mob_dev_portfolio.util.QuantityFormatter

@Composable
fun RecipeDetailScreen(
    onNavigateUp: () -> Unit,
    onItemsAddedToShopping: () -> Unit,
    onError: (String) -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkedSteps by viewModel.checkedSteps.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RecipeDetailEvent.ShoppingListAddSucceeded -> onItemsAddedToShopping()
                is RecipeDetailEvent.ShoppingListAddFailed -> onError(event.message)
                is RecipeDetailEvent.FavouriteUpdateFailed -> onError(event.message)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (uiState is RecipeDetailUiState.Success) {
                AddToListBottomBar(
                    onClick = {
                        viewModel.addToShoppingList()
                    }
                )
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is RecipeDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RecipeDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                }
            }

            is RecipeDetailUiState.Success -> {
                RecipeDetailContent(
                    state = state,
                    checkedSteps = checkedSteps,
                    onNavigateUp = onNavigateUp,
                    onFavouriteClick = { viewModel.toggleFavourite() },
                    onEditClick = onEditClick,
                    onPortionsChanged = { viewModel.setPortions(it) },
                    onInstructionStepToggle = viewModel::toggleStep,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun RecipeDetailContent(
    state: RecipeDetailUiState.Success,
    checkedSteps: Set<Int>,
    onNavigateUp: () -> Unit,
    onFavouriteClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onPortionsChanged: (Int) -> Unit,
    onInstructionStepToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .heightIn(max = 320.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.recipe.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(R.drawable.ic_image_placeholder),
                    error = painterResource(R.drawable.ic_broken_image)
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black.copy(alpha = 0.36f),
                                    0.34f to Color.Transparent
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.navigate_up)
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.recipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onFavouriteClick) {
                        Icon(
                            painter = painterResource(
                                if (state.recipe.isFavourite) R.drawable.ic_favorite
                                else R.drawable.ic_favorite_border
                            ),
                            contentDescription = stringResource(
                                if (state.recipe.isFavourite) {
                                    R.string.remove_from_favourites
                                } else {
                                    R.string.favourite_recipe
                                }
                            ),
                            tint = if (state.recipe.isFavourite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    if (state.recipe.isUserCreated) {
                        IconButton(onClick = { onEditClick(state.recipe.id) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.edit_recipe)
                            )
                        }
                    }
                }
                val meta = listOf(state.recipe.area, state.recipe.category).filter { it.isNotBlank() }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                val showPrepTime = state.recipe.prepTimeMinutes > 0
                val showCookTime = state.recipe.cookTimeMinutes > 0
                if (showPrepTime || showCookTime) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showPrepTime) {
                            AssistChip(
                                onClick = {},
                                label = { Text(stringResource(R.string.recipe_prep_time, state.recipe.prepTimeMinutes)) }
                            )
                        }
                        if (showCookTime) {
                            AssistChip(
                                onClick = {},
                                label = { Text(stringResource(R.string.recipe_cook_time, state.recipe.cookTimeMinutes)) }
                            )
                        }
                    }
                }
            }
        }

        item {
            PortionsSection(
                portions = state.portions,
                onPortionsChanged = onPortionsChanged,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            IngredientsSection(
                ingredients = state.scaledIngredients,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.instructionSteps.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            item {
                Text(
                    text = stringResource(R.string.recipe_instructions_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(
                items = state.instructionSteps,
                key = { _, step -> step.id }
            ) { index, step ->
                InstructionStep(
                    stepNumber = index + 1,
                    text = step.text,
                    isChecked = index in checkedSteps,
                    onToggle = { onInstructionStepToggle(index) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddToListBottomBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shopping_cart),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.add_to_list),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PortionsSection(
    portions: Int,
    onPortionsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.section_portions),
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = portions.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            Slider(
                value = portions.toFloat(),
                onValueChange = { onPortionsChanged(it.toInt()) },
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "20",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IngredientsSection(
    ingredients: List<ScaledIngredient>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Text(
                text = stringResource(R.string.section_ingredients),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
            HorizontalDivider()
            ingredients.forEachIndexed { index, ingredient ->
                IngredientRow(ingredient = ingredient)
                if (index < ingredients.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: ScaledIngredient,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (ingredient.hasQuantity) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = QuantityFormatter.format(
                        ingredient.scaledQuantity,
                        CanonicalUnit.fromSymbol(ingredient.unit)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
