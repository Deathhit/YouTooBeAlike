package com.deathhit.feature.media_item

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.data.media_progress.MediaProgressRepository
import com.deathhit.data.media_progress.model.MediaProgressDO
import com.deathhit.feature.media_item.model.MediaItemLabel
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item.model.toDO
import com.deathhit.feature.media_item.model.toMediaItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaItemListViewModel @Inject constructor(
    mediaItemRepository: MediaItemRepository,
    private val mediaProgressRepository: MediaProgressRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "MediaItemListViewModel"
        private const val KEY_IS_FIRST_PAGE_LOADED = "$TAG.KEY_IS_FIRST_PAGE_LOADED"
        private const val KEY_MEDIA_ITEM_LABEL = "$TAG.KEY_MEDIA_ITEM_LABEL"

        private const val MEDIA_SWITCHING_DELAY = 500L

        fun createArgs(mediaItemLabel: MediaItemLabel) =
            Bundle().apply {
                putSerializable(KEY_MEDIA_ITEM_LABEL, mediaItemLabel)
            }
    }

    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val isFirstPageLoaded: Boolean,
        val isRefreshing: Boolean,
        val mediaItemLabel: MediaItemLabel,
        val playItem: MediaItemVO?,
        val playPosition: Int?
    ) {
        sealed interface Action {
            data class OpenItem(val item: MediaItemVO) : Action
            data class PrepareAndPlayPlayback(
                val isEnded: Boolean,
                val position: Long,
                val sourceUrl: String
            ) : Action

            object ScrollToTop : Action
            object StopPlayback : Action
        }

        data class ListState(
            val isFirstFrameRendered: Boolean,
            val playPosition: Int?
        )

        val listState = ListState(isFirstFrameRendered, playPosition)
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                isFirstFrameRendered = false,
                isFirstPageLoaded = savedStateHandle[KEY_IS_FIRST_PAGE_LOADED] ?: false,
                isRefreshing = false,
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

    private val isFirstPageLoaded get() = stateFlow.value.isFirstPageLoaded
    private val mediaItemLabel get() = stateFlow.value.mediaItemLabel
    private val playItem get() = stateFlow.value.playItem
    private val playPosition get() = stateFlow.value.playPosition

    private var prepareItemJob: Job? = null

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

    fun prepareItem(item: MediaItemVO?) {
        if (item == playItem)
            return

        prepareItemJob?.cancel()
        prepareItemJob = viewModelScope.launch {
            yield() //Allow the assignment of playItem to be canceled.

            _stateFlow.update { state ->
                state.copy(playItem = item)
            }

            delay(MEDIA_SWITCHING_DELAY)

            if (item != null)
                _stateFlow.update { state ->
                    val progress =
                        mediaProgressRepository.getMediaProgressByMediaItemId(item.id)

                    state.copy(
                        actions = state.actions + State.Action.PrepareAndPlayPlayback(
                            progress?.isEnded ?: false,
                            progress?.position ?: 0L,
                            item.sourceUrl
                        )
                    )
                }
        }
    }

    fun savePlayItemPosition(isEnded: Boolean, mediaPosition: Long) {
        playItem?.let {
            viewModelScope.launch(NonCancellable) {
                mediaProgressRepository.setMediaProgress(
                    MediaProgressDO(
                        isEnded,
                        it.id,
                        mediaPosition
                    )
                )
            }
        }
    }

    fun saveState() {
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

    fun setIsRefreshing(isRefreshing: Boolean) {
        _stateFlow.update { state ->
            state.copy(isRefreshing = isRefreshing)
        }
    }

    fun setPlayPosition(playPosition: Int?) {
        if (playPosition == this.playPosition)
            return

        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.StopPlayback,
                isFirstFrameRendered = false,
                playPosition = playPosition
            )
        }

        if (playPosition == null)
            prepareItem(null)
    }
}