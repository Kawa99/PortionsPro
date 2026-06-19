package com.example.mob_dev_portfolio.ui.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mob_dev_portfolio.R
import com.example.mob_dev_portfolio.data.ShoppingListItem
import com.example.mob_dev_portfolio.ui.components.EmptyState
import com.example.mob_dev_portfolio.ui.components.ErrorState
import com.example.mob_dev_portfolio.ui.components.LoadingState
import com.example.mob_dev_portfolio.util.CanonicalUnit
import com.example.mob_dev_portfolio.util.QuantityFormatter

@Composable
fun ShoppingListScreen(
    onError: (String) -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    val otherItemsLabel = stringResource(R.string.shopping_other_items)

    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { message ->
            onError(message)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_all)) },
            text = { Text(stringResource(R.string.shopping_clear_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.clear_all),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.nav_shopping),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )

            when (val state = uiState) {
                ShoppingListUiState.Loading -> {
                    LoadingState(modifier = Modifier.weight(1f))
                }
                ShoppingListUiState.Empty -> {
                    EmptyState(
                        iconRes = R.drawable.ic_shopping_cart,
                        title = stringResource(R.string.shopping_empty_title),
                        message = stringResource(R.string.shopping_empty_message),
                        modifier = Modifier.weight(1f)
                    )
                }
                is ShoppingListUiState.Error -> {
                    val fallbackMessage = stringResource(R.string.shopping_load_error_message)
                    ErrorState(
                        iconRes = R.drawable.ic_broken_image,
                        title = stringResource(R.string.shopping_load_error_title),
                        message = state.message.ifBlank { fallbackMessage },
                        actionLabel = stringResource(R.string.retry),
                        onActionClick = viewModel::retry,
                        modifier = Modifier.weight(1f)
                    )
                }
                is ShoppingListUiState.Content -> {
                    ShoppingControls(
                        items = state.items,
                        onClearChecked = viewModel::clearChecked,
                        onClearAll = { showClearDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        state.sections.forEach { section ->
                            val headerText = if (section.sourceName == ShoppingListViewModel.OTHER_ITEMS_SOURCE_NAME) {
                                otherItemsLabel
                            } else {
                                section.sourceName
                            }
                            item(key = "header-${section.sourceName}") {
                                Text(
                                    text = headerText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }
                            items(section.items, key = { it.id }) { item ->
                                ShoppingListRow(
                                    item = item,
                                    onToggle = { viewModel.toggleChecked(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingControls(
    items: List<ShoppingListItem>,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkedCount = items.count { it.isChecked }
    val totalCount = items.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.shopping_progress, checkedCount, totalCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (totalCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (checkedCount > 0) {
                        TextButton(onClick = onClearChecked) {
                            Text(stringResource(R.string.shopping_clear_checked))
                        }
                    }
                    IconButton(onClick = onClearAll) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.clear_all)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingListRow(
    item: ShoppingListItem,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (item.isChecked) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (item.isChecked) 0.dp else 1.dp,
        shadowElevation = if (item.isChecked) 0.dp else 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.ingredientName,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    color = if (item.isChecked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (item.sourceRecipeName.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.shopping_from_recipe, item.sourceRecipeName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (item.quantity > 0.0) {
                QuantityPill(
                    text = QuantityFormatter.format(
                        item.quantity,
                        CanonicalUnit.fromSymbol(item.unit)
                    )
                )
            }
        }
    }
}

@Composable
private fun QuantityPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(start = 12.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
