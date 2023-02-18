package com.deathhit.feature.media_item.fragment.media_item

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.repository.MediaItemRepository
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item.model.toMediaItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MediaItemListViewModel @Inject constructor(
    mediaItemRepository: MediaItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "MediaItemListViewModel"
        private const val KEY_IS_PLAYING_IN_LIST = "$TAG.KEY_IS_PLAYING_IN_LIST"

        fun createArgs(isPlayingInList: Boolean) = Bundle().apply {
            putBoolean(KEY_IS_PLAYING_IN_LIST, isPlayingInList)
        }
    }

    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val isFirstPageLoaded: Boolean,
        val isPlayingInList: Boolean,
        val playPosition: Int?
    ) {
        sealed interface Action {
            data class OpenItem(val item: MediaItemVO) : Action
            data class PrepareItem(val item: MediaItemVO?) : Action
            object ScrollToTop : Action
        }
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                isFirstFrameRendered = false,
                isFirstPageLoaded = false,
                isPlayingInList = savedStateHandle[KEY_IS_PLAYING_IN_LIST] ?: true,
                playPosition = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val mediaItemPagingDataFlow =
        mediaItemRepository.getMediaItemPagingDataFlow()
            .map { pagingData -> pagingData.map { it.toMediaItemVO() } }
            .cachedIn(viewModelScope)

    private val isFirstPageLoaded get() = stateFlow.value.isFirstPageLoaded
    private val isPlayingInList get() = stateFlow.value.isPlayingInList

    fun onAction(action: State.Action) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions - action)
        }
    }

    fun openItem(item: MediaItemVO) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.OpenItem(item))
        }
    }

    fun prepareItem(item: MediaItemVO?) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.PrepareItem(item))
        }
    }

    fun saveState() {
        savedStateHandle[KEY_IS_PLAYING_IN_LIST] = isPlayingInList
    }

    fun scrollToTopOnFirstPageLoaded() {
        if (isFirstPageLoaded)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ScrollToTop, isFirstPageLoaded = true)
        }
    }

    fun setIsFirstFrameRendered(isFirstFrameRendered: Boolean) {
        _stateFlow.update { state ->
            state.copy(isFirstFrameRendered = isFirstFrameRendered)
        }
    }

    fun setIsPlayingInList(isPlayingInList: Boolean) {
        _stateFlow.update { state ->
            state.copy(isPlayingInList = isPlayingInList)
        }
    }

    fun setPlayPosition(playPosition: Int?) {
        _stateFlow.update { state ->
            state.copy(playPosition = playPosition)
        }

        if (playPosition == null)
            prepareItem(null)
    }
}