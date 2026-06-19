package com.example.mob_dev_portfolio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.mob_dev_portfolio.R
import com.example.mob_dev_portfolio.data.RecipeSummaryView

@Composable
fun RecipeCard(
    recipe: RecipeSummaryView,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(recipe.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.ic_image_placeholder),
                    error = painterResource(R.drawable.ic_broken_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (recipe.isFavourite) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_favorite),
                            contentDescription = stringResource(R.string.favourite_recipe),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = listOf(recipe.area, recipe.category).filter { it.isNotBlank() }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (recipe.ingredientCount > 0 || recipe.totalTimeMinutes > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (recipe.ingredientCount > 0) {
                            RecipeInfoChip(
                                text = stringResource(R.string.ingredient_count, recipe.ingredientCount)
                            )
                        }
                        if (recipe.totalTimeMinutes > 0) {
                            RecipeInfoChip(
                                text = stringResource(R.string.recipe_total_time, recipe.totalTimeMinutes)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeInfoChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
