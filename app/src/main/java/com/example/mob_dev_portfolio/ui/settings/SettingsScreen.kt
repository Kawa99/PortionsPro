package com.example.mob_dev_portfolio.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mob_dev_portfolio.R
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onError: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val defaultPortions by viewModel.defaultPortions.collectAsStateWithLifecycle()
    val useMetric by viewModel.useMetric.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is SettingsEvent.Error) {
                onError(event.message)
            }
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.nav_settings),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                SettingsSectionTitle(text = stringResource(R.string.settings_meal_prep_section))
                SettingsSection {
                    PortionsSettingRow(
                        value = defaultPortions,
                        onValueChanged = viewModel::setDefaultPortions
                    )
                }
            }

            item {
                SettingsSectionTitle(text = stringResource(R.string.settings_preferences_section))
                SettingsSection {
                    UnitsSettingRow(
                        useMetric = useMetric,
                        onUseMetricChanged = viewModel::setUseMetric
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(content = content)
    }
}

@Composable
private fun PortionsSettingRow(
    value: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_default_portions),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.settings_default_portions_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChanged(it.roundToInt()) },
            valueRange = 1f..20f,
            steps = 18
        )
    }
}

@Composable
private fun UnitsSettingRow(
    useMetric: Boolean,
    onUseMetricChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_units),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.settings_units_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            SegmentedButton(
                selected = useMetric,
                onClick = { onUseMetricChanged(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.settings_metric))
            }
            SegmentedButton(
                selected = !useMetric,
                onClick = { onUseMetricChanged(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(stringResource(R.string.settings_imperial))
            }
        }
    }
}
