package com.deathhit.feature.media_item

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.feature.media_item.model.MediaItemLabel
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item.model.toDO
import com.deathhit.feature.media_item.model.toMediaItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaItemListViewModel @Inject constructor(
    mediaItemRepository: MediaItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "MediaItemListViewModel"
        private const val KEY_IS_PLAYING = "$TAG.KEY_IS_PLAYING"
        private const val KEY_MEDIA_ITEM_SOURCE_TYPE = "$TAG.KEY_MEDIA_ITEM_SOURCE_TYPE"

        fun createArgs(isPlaying: Boolean, mediaItemLabel: MediaItemLabel) =
            Bundle().apply {
                putBoolean(KEY_IS_PLAYING, isPlaying)
                putSerializable(KEY_MEDIA_ITEM_SOURCE_TYPE, mediaItemLabel)
            }
    }

    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val isFirstPageLoaded: Boolean,
        val isPlaying: Boolean,
        val mediaItemLabel: MediaItemLabel,
        val playPosition: Int?
    ) {
        sealed interface Action {
            data class OpenItem(val item: MediaItemVO) : Action
            data class PrepareItemAndPlay(val item: MediaItemVO?) : Action
            object ScrollToTop : Action
        }
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                isFirstFrameRendered = false,
                isFirstPageLoaded = false,
                isPlaying = savedStateHandle[KEY_IS_PLAYING] ?: true,
                mediaItemLabel = savedStateHandle[KEY_MEDIA_ITEM_SOURCE_TYPE]
                    ?: throw RuntimeException("mediaItemLabel can not be null!"),
                playPosition = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val mediaItemPagingDataFlow =
        stateFlow.map { it.mediaItemLabel }.distinctUntilChanged().flatMapLatest {
            mediaItemRepository.getMediaItemPagingDataFlow(mediaItemLabel = mediaItemSourceType.toDO())
                .map { pagingData -> pagingData.map { it.toMediaItemVO() } }
        }.cachedIn(viewModelScope)

    private val isFirstPageLoaded get() = stateFlow.value.isFirstPageLoaded
    private val isPlaying get() = stateFlow.value.isPlaying
    private val mediaItemSourceType get() = stateFlow.value.mediaItemLabel

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

    fun prepareItemAndPlay(item: MediaItemVO?) {
        if (!isPlaying)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.PrepareItemAndPlay(item))
        }
    }

    fun saveState() {
        savedStateHandle[KEY_IS_PLAYING] = isPlaying
        savedStateHandle[KEY_MEDIA_ITEM_SOURCE_TYPE] = mediaItemSourceType
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

    fun setIsPlaying(isPlaying: Boolean) {
        _stateFlow.update { state ->
            state.copy(isPlaying = isPlaying)
        }
    }

    fun setPlayPosition(playPosition: Int?) {
        _stateFlow.update { state ->
            state.copy(playPosition = playPosition)
        }

        if (playPosition == null)
            prepareItemAndPlay(null)
    }
}