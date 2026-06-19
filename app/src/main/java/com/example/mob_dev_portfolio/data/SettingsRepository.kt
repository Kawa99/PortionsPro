package com.example.mob_dev_portfolio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepositoryInterface {

    companion object {
        val DEFAULT_PORTIONS = intPreferencesKey("default_portions")
        val USE_METRIC = booleanPreferencesKey("use_metric")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    override val defaultPortions: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[DEFAULT_PORTIONS] ?: 2 }

    override val useMetric: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[USE_METRIC] ?: true }

    override val onboardingComplete: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[ONBOARDING_COMPLETE] ?: false }

    override suspend fun setDefaultPortions(value: Int) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { prefs -> prefs[DEFAULT_PORTIONS] = value }
        }
    }

    override suspend fun setUseMetric(value: Boolean) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { prefs -> prefs[USE_METRIC] = value }
        }
    }

    override suspend fun setOnboardingComplete() {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETE] = true }
        }
    }
}
