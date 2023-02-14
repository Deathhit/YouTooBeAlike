package com.deathhit.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeActivityViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle) :
    ViewModel() {
    companion object {
        private const val TAG = "HomeActivityViewModel"
        private const val KEY_TAB = "$TAG.KEY_TAB"
    }

    data class State(val tab: Tab) {
        enum class Tab {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }
    }

    private val _stateFlow =
        MutableStateFlow(State(tab = savedStateHandle[KEY_TAB] ?: State.Tab.HOME))
    val stateFlow = _stateFlow.asStateFlow()

    private val tab get() = stateFlow.value.tab

    fun saveState() {
        savedStateHandle[KEY_TAB] = tab
    }
}