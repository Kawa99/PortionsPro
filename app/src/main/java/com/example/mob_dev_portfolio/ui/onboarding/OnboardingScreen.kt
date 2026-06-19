package com.example.mob_dev_portfolio.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mob_dev_portfolio.R
import com.example.mob_dev_portfolio.ui.settings.SettingsEvent
import com.example.mob_dev_portfolio.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val visual: OnboardingVisual,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

private enum class OnboardingVisual {
    Prep,
    Scale,
    Shop
}

private val pages = listOf(
    OnboardingPage(OnboardingVisual.Prep, R.string.onboarding_browse_title, R.string.onboarding_browse_body),
    OnboardingPage(OnboardingVisual.Scale, R.string.onboarding_scale_title, R.string.onboarding_scale_body),
    OnboardingPage(OnboardingVisual.Shop, R.string.onboarding_favourites_title, R.string.onboarding_favourites_body),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onError: (String) -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.events.collect { event ->
            when (event) {
                SettingsEvent.OnboardingCompleted -> onFinished()
                is SettingsEvent.Error -> onError(event.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                val color by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    label = "dot_color"
                )
                Box(
                    modifier = Modifier
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            onClick = {
                if (isLastPage) {
                    settingsViewModel.setOnboardingComplete()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            }
        ) {
            Text(
                if (isLastPage) {
                    stringResource(R.string.onboarding_get_started)
                } else {
                    stringResource(R.string.onboarding_next)
                }
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(28.dp))
        OnboardingVisual(page.visual)
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun OnboardingVisual(visual: OnboardingVisual) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (visual) {
                OnboardingVisual.Prep -> MealPrepVisual()
                OnboardingVisual.Scale -> ScaleVisual()
                OnboardingVisual.Shop -> ShoppingVisual()
            }
        }
    }
}

@Composable
private fun MealPrepVisual() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { index ->
                    MealCompartment(
                        modifier = Modifier
                            .weight(1f)
                            .height(86.dp),
                        selected = index == 0
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            MealCompartment(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                selected = true
            )
        }
    }
}

@Composable
private fun MealCompartment(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_restaurant),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun ScaleVisual() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.onboarding_preview_portions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = "8",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(modifier = Modifier.height(26.dp))
            IngredientPreview(
                name = stringResource(R.string.onboarding_preview_rice),
                amount = stringResource(R.string.onboarding_preview_rice_amount)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            IngredientPreview(
                name = stringResource(R.string.onboarding_preview_chicken),
                amount = stringResource(R.string.onboarding_preview_chicken_amount)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            IngredientPreview(
                name = stringResource(R.string.onboarding_preview_broccoli),
                amount = stringResource(R.string.onboarding_preview_broccoli_amount)
            )
        }
    }
}

@Composable
private fun IngredientPreview(name: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ShoppingVisual() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_shopping_cart),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_preview_shopping_list),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            ShoppingPreviewRow(
                text = stringResource(R.string.onboarding_preview_chicken_breast),
                checked = true
            )
            Spacer(modifier = Modifier.height(14.dp))
            ShoppingPreviewRow(
                text = stringResource(R.string.onboarding_preview_rice),
                checked = true
            )
            Spacer(modifier = Modifier.height(14.dp))
            ShoppingPreviewRow(
                text = stringResource(R.string.onboarding_preview_fresh_greens),
                checked = false
            )
        }
    }
}

@Composable
private fun ShoppingPreviewRow(text: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
