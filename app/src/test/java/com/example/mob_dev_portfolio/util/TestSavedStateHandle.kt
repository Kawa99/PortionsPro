package com.example.mob_dev_portfolio.util

import androidx.lifecycle.SavedStateHandle

/**
 * Creates a SavedStateHandle pre-populated with the given key/value pairs.
 * Used in ViewModel tests to simulate navigation arguments.
 */
fun savedStateHandleOf(vararg pairs: Pair<String, Any?>): SavedStateHandle {
    return SavedStateHandle(pairs.toMap())
}
