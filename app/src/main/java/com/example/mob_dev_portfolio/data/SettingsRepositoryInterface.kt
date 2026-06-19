package com.example.mob_dev_portfolio.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepositoryInterface {
    val useMetric: Flow<Boolean>
    val defaultPortions: Flow<Int>
    val onboardingComplete: Flow<Boolean>
    suspend fun setUseMetric(value: Boolean)
    suspend fun setDefaultPortions(value: Int)
    suspend fun setOnboardingComplete()
}
