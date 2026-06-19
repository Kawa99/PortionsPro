package com.example.mob_dev_portfolio.ui.import_recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mob_dev_portfolio.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportRecipeScreen(
    onNavigateUp: () -> Unit,
    onNavigateToDraft: (String) -> Unit,
    viewModel: ImportRecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val url by viewModel.url.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ImportUiState.Success) {
            onNavigateToDraft(state.draftId)
            viewModel.markNavigationHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.import_recipe_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.import_recipe_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = url,
                        onValueChange = viewModel::onUrlChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.import_recipe_url_hint)) },
                        placeholder = { Text(stringResource(R.string.import_recipe_url_placeholder)) },
                        singleLine = true,
                        enabled = uiState !is ImportUiState.Loading,
                        isError = uiState is ImportUiState.Error,
                        supportingText = {
                            val state = uiState
                            if (state is ImportUiState.Error) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        trailingIcon = {
                            if (url.isBlank()) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val clipData = clipboard.getClipEntry()?.clipData
                                            val text = clipData
                                                ?.takeIf { it.itemCount > 0 }
                                                ?.getItemAt(0)
                                                ?.coerceToText(context)
                                                ?.toString()
                                            if (!text.isNullOrBlank()) {
                                                viewModel.onUrlChanged(text)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add_circle),
                                        contentDescription = stringResource(R.string.import_recipe_paste)
                                    )
                                }
                            } else {
                                IconButton(onClick = { viewModel.onUrlChanged("") }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = stringResource(R.string.import_recipe_clear)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                keyboard?.hide()
                                viewModel.importFromUrl()
                            }
                        )
                    )

                    Button(
                        onClick = {
                            keyboard?.hide()
                            viewModel.importFromUrl()
                        },
                        enabled = url.isNotBlank() && uiState !is ImportUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is ImportUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.import_recipe_loading))
                        } else {
                            Text(stringResource(R.string.import_recipe_button))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.import_recipe_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}
