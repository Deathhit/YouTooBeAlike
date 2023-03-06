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
        private const val KEY_IS_FIRST_FRAME_RENDERED = "$TAG.KEY_IS_FIRST_FRAME_RENDERED"
        private const val KEY_IS_FIRST_PAGE_LOADED = "$TAG.KEY_IS_FIRST_PAGE_LOADED"
        private const val KEY_MEDIA_ITEM_LABEL = "$TAG.KEY_MEDIA_ITEM_LABEL"

        fun createArgs(mediaItemLabel: MediaItemLabel) =
            Bundle().apply {
                putSerializable(KEY_MEDIA_ITEM_LABEL, mediaItemLabel)
            }
    }

    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val isFirstPageLoaded: Boolean,
        val isPlayerSet: Boolean,
        val mediaItemLabel: MediaItemLabel,
        val playItem: MediaItemVO?,
        val playPosition: Int?
    ) {
        sealed interface Action {
            data class OpenItem(val item: MediaItemVO) : Action
            data class PrepareItemAndPlay(val item: MediaItemVO?) : Action
            object ScrollToTop : Action
        }

        data class ListState(
            val isFirstFrameRendered: Boolean,
            val isPlayerSet: Boolean,
            val playPosition: Int?
        )

        val listState = ListState(isFirstFrameRendered, isPlayerSet, playPosition)
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                isFirstFrameRendered = savedStateHandle[KEY_IS_FIRST_FRAME_RENDERED]
                    ?: false,  //todo do we need to save this?
                isFirstPageLoaded = savedStateHandle[KEY_IS_FIRST_PAGE_LOADED] ?: false,
                isPlayerSet = false,
                mediaItemLabel = savedStateHandle[KEY_MEDIA_ITEM_LABEL]
                    ?: throw RuntimeException("mediaItemLabel can not be null!"),
                playItem = null,
                playPosition = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val mediaItemPagingDataFlow =
        stateFlow.map { it.mediaItemLabel }.distinctUntilChanged().flatMapLatest {
            mediaItemRepository.getMediaItemPagingDataFlow(mediaItemLabel = mediaItemLabel.toDO())
                .map { pagingData -> pagingData.map { it.toMediaItemVO() } }
        }.cachedIn(viewModelScope)

    private val isFirstFrameRendered get() = stateFlow.value.isFirstFrameRendered
    private val isFirstPageLoaded get() = stateFlow.value.isFirstPageLoaded
    private val isPlayerSet get() = stateFlow.value.isPlayerSet
    private val mediaItemLabel get() = stateFlow.value.mediaItemLabel
    private val playItem get() = stateFlow.value.playItem
    private val playPosition get() = stateFlow.value.playPosition

    fun notifyFirstFrameRendered() {
        _stateFlow.update { state ->
            state.copy(isFirstFrameRendered = true)
        }
    }

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
        if (!isPlayerSet || item == playItem)
            return

        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.PrepareItemAndPlay(item),
                playItem = item
            )
        }
    }

    fun saveState() {
        savedStateHandle[KEY_IS_FIRST_FRAME_RENDERED] = isFirstFrameRendered
        savedStateHandle[KEY_IS_FIRST_PAGE_LOADED] = isFirstPageLoaded
        savedStateHandle[KEY_MEDIA_ITEM_LABEL] = mediaItemLabel
    }

    fun scrollToTopOnFirstPageLoaded() {
        if (isFirstPageLoaded)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ScrollToTop, isFirstPageLoaded = true)
        }
    }

    fun setIsPlayerSet(isPlayerSet: Boolean) {
        if (isPlayerSet == this.isPlayerSet)
            return

        _stateFlow.update { state ->
            state.copy(isPlayerSet = isPlayerSet, playItem = null)
        }
    }

    fun setPlayPosition(playPosition: Int?) {
        if (playPosition == this.playPosition)
            return

        _stateFlow.update { state ->
            state.copy(isFirstFrameRendered = false, playPosition = playPosition)
        }

        if (playPosition == null)
            prepareItemAndPlay(null)
    }
}