package com.example.mob_dev_portfolio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.mob_dev_portfolio.R

@Composable
fun InstructionStep(
    stepNumber: Int,
    text: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepTextColor by animateColorAsState(
        targetValue = if (isChecked) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "instructionStepTextColor"
    )
    val stepIndicatorColor by animateColorAsState(
        targetValue = if (isChecked) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        label = "instructionStepIndicatorColor"
    )
    val completionStateDescription = stringResource(
        if (isChecked) {
            R.string.instruction_step_completed
        } else {
            R.string.instruction_step_not_completed
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                stateDescription = completionStateDescription
            }
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(stepIndicatorColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isChecked) "✓" else stepNumber.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (isChecked) TextDecoration.LineThrough else null
            ),
            color = stepTextColor,
            modifier = Modifier
                .weight(1f)
                .alpha(if (isChecked) 0.6f else 1f)
        )
    }
}
