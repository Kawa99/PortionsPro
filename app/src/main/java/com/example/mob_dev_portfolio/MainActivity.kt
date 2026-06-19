package com.example.mob_dev_portfolio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.mob_dev_portfolio.data.SettingsRepository
import com.example.mob_dev_portfolio.ui.navigation.AppNavGraph
import com.example.mob_dev_portfolio.ui.navigation.Onboarding
import com.example.mob_dev_portfolio.ui.navigation.RecipeList
import com.example.mob_dev_portfolio.ui.theme.PortionsProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            PortionsProTheme {
                var startDestination by remember { mutableStateOf<Any?>(null) }

                LaunchedEffect(Unit) {
                    val complete = settingsRepository.onboardingComplete.first()
                    startDestination = if (complete) RecipeList else Onboarding
                }

                val navController = rememberNavController()
                val startDest = startDestination
                if (startDest != null) {
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDest
                    )
                }
            }
        }
    }
}
