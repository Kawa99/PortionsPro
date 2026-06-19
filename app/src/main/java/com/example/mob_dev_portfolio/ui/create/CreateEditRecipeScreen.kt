package com.example.mob_dev_portfolio.ui.create

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.mob_dev_portfolio.R
import java.io.File
import java.util.UUID

private val commonIngredientUnits = listOf(
    "",
    "g",
    "kg",
    "ml",
    "l",
    "tsp",
    "tbsp",
    "cup",
    "oz",
    "lb",
    "fl oz",
    "piece"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditRecipeScreen(
    onCancel: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    onNavigateToImport: () -> Unit,
    recipeId: String = "NEW",
    importDraftId: String? = null,
    viewModel: CreateEditRecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadedRecipe by viewModel.loadedRecipe.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val formErrors by viewModel.formErrors.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isInstructionSectionExpanded by remember { mutableStateOf(true) }
    var expandedIngredientIndexes by remember { mutableStateOf(setOf(0)) }
    var pendingInstructionFocusIndex by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importRecipeImage(it)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraImageUri
        if (success && uri != null) {
            viewModel.importRecipeImage(uri)
        }
    }

    LaunchedEffect(recipeId) {
        if (recipeId != "NEW") {
            viewModel.loadRecipe(recipeId)
        }
    }

    LaunchedEffect(importDraftId) {
        val draftId = importDraftId ?: return@LaunchedEffect
        if (recipeId != "NEW") return@LaunchedEffect

        viewModel.loadImportDraft(draftId)
    }

    LaunchedEffect(formState.ingredientRows) {
        expandedIngredientIndexes = expandedIngredientIndexes
            .filter { it in formState.ingredientRows.indices }
            .toSet() +
            formState.ingredientRows.mapIndexedNotNull { index, row ->
                if (!row.isComplete) index else null
            }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CreateEditUiState.Saved -> {
                keyboard?.hide()
                onSaved(state.recipeId)
            }

            is CreateEditUiState.Deleted -> {
                keyboard?.hide()
                onDeleted()
            }

            is CreateEditUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetUiState()
            }

            else -> Unit
        }
    }

    val isEditing = loadedRecipe != null
    val isUserCreated = loadedRecipe?.recipe?.isUserCreated == true

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.create_delete_title)) },
            text = { Text(stringResource(R.string.create_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecipe()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.create_delete_recipe),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        if (isEditing) stringResource(R.string.create_edit_title)
                        else stringResource(R.string.create_new_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    if (isUserCreated) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.create_delete_recipe)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FormSection(title = stringResource(R.string.create_photo)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(formState.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.recipe_thumbnail),
                        placeholder = painterResource(R.drawable.ic_image_placeholder),
                        error = painterResource(R.drawable.ic_image_placeholder),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.create_choose_gallery))
                        }
                        OutlinedButton(
                            onClick = {
                                val uri = createCameraImageUri(context)
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.create_take_photo))
                        }
                    }
                    if (formState.thumbnailUrl.isNotBlank()) {
                        TextButton(onClick = { viewModel.onThumbnailUrlChanged("") }) {
                            Text(stringResource(R.string.create_remove_photo))
                        }
                    }
                }
            }

            item {
                FormSection(title = stringResource(R.string.create_basics)) {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text(stringResource(R.string.create_recipe_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formErrors.name,
                        supportingText = if (formErrors.name) {
                            { Text(stringResource(R.string.create_name_required)) }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = formState.category,
                        onValueChange = viewModel::onCategoryChanged,
                        label = { Text(stringResource(R.string.create_category_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = formState.baseServings,
                        onValueChange = viewModel::onBaseServingsChanged,
                        label = { Text(stringResource(R.string.create_base_servings)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formErrors.servings,
                        supportingText = if (formErrors.servings) {
                            { Text(stringResource(R.string.create_servings_required)) }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.create_ingredients),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            val nextIndex = formState.ingredientRows.size
                            viewModel.addIngredientRow()
                            expandedIngredientIndexes = expandedIngredientIndexes + nextIndex
                        }
                    ) {
                        Text(stringResource(R.string.create_add_ingredient))
                    }
                }
            }

            itemsIndexed(
                items = formState.ingredientRows,
                key = { _, row -> row.id }
            ) { index, row ->
                val isExpanded = index in expandedIngredientIndexes || !row.isComplete
                IngredientInputCard(
                    row = row,
                    isExpanded = isExpanded,
                    isQuantityError = index in formErrors.ingredientQuantityErrorIndexes,
                    onExpandedChanged = { expanded ->
                        expandedIngredientIndexes = if (expanded) {
                            expandedIngredientIndexes + index
                        } else {
                            expandedIngredientIndexes - index
                        }
                    },
                    onNameChanged = { new -> viewModel.onIngredientNameChanged(index, new) },
                    onQuantityChanged = { new -> viewModel.onIngredientQuantityChanged(index, new) },
                    onUnitChanged = { new -> viewModel.onIngredientUnitChanged(index, new) },
                    onRemove = {
                        val currentExpandedIndexes = expandedIngredientIndexes
                        val nextSize = (formState.ingredientRows.size - 1).coerceAtLeast(1)
                        viewModel.removeIngredientRow(index)
                        expandedIngredientIndexes = (0 until nextSize)
                            .filter { nextIndex ->
                                val nextRow = formState.ingredientRows
                                    .filterIndexed { rowIndex, _ -> rowIndex != index }
                                    .ifEmpty { listOf(IngredientInputRow()) }[nextIndex]
                                !nextRow.isComplete ||
                                    when {
                                        nextIndex < index -> nextIndex in currentExpandedIndexes
                                        else -> nextIndex + 1 in currentExpandedIndexes
                                    }
                            }
                            .toSet()
                    }
                )
            }

            item {
                InstructionStepsSection(
                    rows = formState.instructionRows,
                    isExpanded = isInstructionSectionExpanded,
                    stepCount = formState.instructionRows.count { it.text.isNotBlank() },
                    onExpandedChanged = { isInstructionSectionExpanded = it },
                    onAddStep = {
                        val nextIndex = formState.instructionRows.size
                        viewModel.addInstructionRow()
                        isInstructionSectionExpanded = true
                        pendingInstructionFocusIndex = nextIndex
                    },
                    pendingFocusIndex = pendingInstructionFocusIndex,
                    onPendingFocusConsumed = { pendingInstructionFocusIndex = null },
                    onStepChanged = viewModel::onInstructionChanged,
                    onStepRemoved = viewModel::removeInstructionRow,
                    onNextFromStep = { index ->
                        val nextIndex = index + 1
                        if (nextIndex < formState.instructionRows.size) {
                            pendingInstructionFocusIndex = nextIndex
                        } else {
                            viewModel.addInstructionRow()
                            isInstructionSectionExpanded = true
                            pendingInstructionFocusIndex = nextIndex
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                val isSaving = uiState is CreateEditUiState.Saving
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    onClick = {
                        val errors = viewModel.saveRecipe(
                            recipeId = loadedRecipe?.recipe?.id ?: "NEW"
                        )
                        if (errors.ingredientQuantityErrorIndexes.isNotEmpty()) {
                            expandedIngredientIndexes = expandedIngredientIndexes +
                                errors.ingredientQuantityErrorIndexes
                        }
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.create_save_recipe))
                    }
                }
                if (recipeId == "NEW") {
                    TextButton(
                        onClick = onNavigateToImport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.import_recipe_or_link))
                    }
                }
            }
        }
    }
}

private fun createCameraImageUri(context: Context): Uri {
    val imageDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imageDir, "camera-${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Composable
private fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun InstructionStepsSection(
    rows: List<InstructionInputRow>,
    isExpanded: Boolean,
    stepCount: Int,
    onExpandedChanged: (Boolean) -> Unit,
    onAddStep: () -> Unit,
    pendingFocusIndex: Int?,
    onPendingFocusConsumed: () -> Unit,
    onStepChanged: (Int, String) -> Unit,
    onStepRemoved: (Int) -> Unit,
    onNextFromStep: (Int) -> Unit,
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChanged(!isExpanded) }
                    .padding(start = 16.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.create_instructions_hint),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = instructionStepSummary(stepCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TextButton(onClick = onAddStep) {
                    Text(stringResource(R.string.create_add_instruction_step))
                }
                IconButton(onClick = { onExpandedChanged(!isExpanded) }) {
                    Icon(
                        painter = painterResource(
                            if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                        ),
                        contentDescription = stringResource(
                            if (isExpanded) R.string.collapse else R.string.expand
                        )
                    )
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rows.forEachIndexed { index, row ->
                        key(row.id) {
                            val stepFocusRequester = remember { FocusRequester() }
                            LaunchedEffect(pendingFocusIndex) {
                                if (pendingFocusIndex == index) {
                                    stepFocusRequester.requestFocus()
                                    onPendingFocusConsumed()
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                OutlinedTextField(
                                    value = row.text,
                                    onValueChange = { onStepChanged(index, it) },
                                    label = {
                                        Text(stringResource(R.string.create_instruction_step_label, index + 1))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(stepFocusRequester),
                                    minLines = 1,
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { onNextFromStep(index) }
                                    )
                                )
                                IconButton(onClick = { onStepRemoved(index) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = stringResource(
                                            R.string.create_remove_instruction_step
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun instructionStepSummary(stepCount: Int): String {
    return when (stepCount) {
        0 -> stringResource(R.string.create_no_instruction_steps)
        1 -> stringResource(R.string.create_one_instruction_step)
        else -> stringResource(R.string.create_instruction_step_count, stepCount)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientInputCard(
    row: IngredientInputRow,
    isExpanded: Boolean,
    isQuantityError: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (isExpanded) {
            ExpandedIngredientContent(
                row = row,
                isQuantityError = isQuantityError,
                onExpandedChanged = onExpandedChanged,
                onNameChanged = onNameChanged,
                onQuantityChanged = onQuantityChanged,
                onUnitChanged = onUnitChanged,
                onRemove = onRemove
            )
        } else {
            CollapsedIngredientContent(
                row = row,
                onExpandedChanged = onExpandedChanged,
                onRemove = onRemove
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedIngredientContent(
    row: IngredientInputRow,
    isQuantityError: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = row.name,
                onValueChange = onNameChanged,
                label = { Text(stringResource(R.string.create_ingredient_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            IconButton(
                onClick = { if (row.isComplete) onExpandedChanged(false) },
                enabled = row.isComplete,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_expand_less),
                    contentDescription = stringResource(R.string.collapse)
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.create_remove_ingredient)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = row.quantity,
                onValueChange = onQuantityChanged,
                label = { Text(stringResource(R.string.create_quantity_hint)) },
                modifier = Modifier.weight(1f),
                isError = isQuantityError,
                supportingText = if (isQuantityError) {
                    { Text(stringResource(R.string.create_quantity_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
            UnitDropdownField(
                value = row.unit,
                onValueChanged = onUnitChanged,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CollapsedIngredientContent(
    row: IngredientInputRow,
    onExpandedChanged: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChanged(true) }
            .padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name.ifBlank { stringResource(R.string.create_ingredient_hint) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatIngredientAmount(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        IconButton(onClick = { onExpandedChanged(true) }) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = stringResource(R.string.expand)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.create_remove_ingredient)
            )
        }
    }
}

private fun formatIngredientAmount(row: IngredientInputRow): String {
    return when {
        row.quantity.isBlank() -> ""
        row.unit.isBlank() -> row.quantity
        else -> "${row.quantity} ${row.unit}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdownField(
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            label = { Text(stringResource(R.string.create_unit_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable)
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            commonIngredientUnits.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = unit.ifBlank {
                                stringResource(R.string.create_unit_none)
                            }
                        )
                    },
                    onClick = {
                        onValueChanged(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
