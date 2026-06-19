package com.example.mob_dev_portfolio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mob_dev_portfolio.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsEvent {
    data object OnboardingCompleted : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    val defaultPortions: StateFlow<Int> = settingsRepository.defaultPortions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 2
        )

    val useMetric: StateFlow<Boolean> = settingsRepository.useMetric
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    fun setDefaultPortions(value: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setDefaultPortions(value)
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Error(e.message ?: "Failed to save default portions"))
            }
        }
    }

    fun setUseMetric(value: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setUseMetric(value)
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Error(e.message ?: "Failed to save unit preference"))
            }
        }
    }

    fun setOnboardingComplete() {
        viewModelScope.launch {
            try {
                settingsRepository.setOnboardingComplete()
                _events.emit(SettingsEvent.OnboardingCompleted)
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Error(e.message ?: "Failed to save onboarding preference"))
            }
        }
    }
}
