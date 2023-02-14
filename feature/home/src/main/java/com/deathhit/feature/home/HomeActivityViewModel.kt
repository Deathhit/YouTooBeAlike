package com.deathhit.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.deathhit.feature.video_list.model.VideoVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeActivityViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle) :
    ViewModel() {
    companion object {
        private const val TAG = "HomeActivityViewModel"
        private const val KEY_CURRENT_VIDEO = "$TAG.KEY_CURRENT_VIDEO"
        private const val KEY_TAB = "$TAG.KEY_TAB"
    }

    data class State(val currentVideo: VideoVO?, val tab: Tab) {
        enum class Tab {
            DASHBOARD,
            HOME,
            NOTIFICATIONS
        }
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                currentVideo = null,
                tab = savedStateHandle[KEY_TAB] ?: State.Tab.HOME
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    private val currentVideo get() = stateFlow.value.currentVideo
    private val tab get() = stateFlow.value.tab

    fun playVideo(item: VideoVO) {
        _stateFlow.update { state ->
            state.copy(currentVideo = item)
        }
    }

    fun setTab(tab: State.Tab) {
        _stateFlow.update { state ->
            state.copy(tab = tab)
        }
    }

    fun saveState() {
        savedStateHandle[KEY_CURRENT_VIDEO] = currentVideo
        savedStateHandle[KEY_TAB] = tab
    }
}