package com.deathhit.feature.playback_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.model.MediaItemLabel
import com.deathhit.data.media_item.MediaItemRepository
import com.deathhit.feature.media_item_list.model.MediaItemVO
import com.deathhit.feature.media_item_list.model.toMediaItemVO
import com.deathhit.feature.playback_details.model.PlaybackDetailsVO
import com.deathhit.feature.playback_details.model.toPlaybackDetailsVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaybackDetailsViewModel @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "PlaybackDetailsViewModel"
        private const val KEY_PLAY_ITEM_ID = "$TAG.KEY_PLAY_ITEM_ID"
    }

    data class State(
        val actions: List<Action>,
        val playbackDetails: PlaybackDetailsVO?,
        val playItemId: String?
    ) {
        sealed interface Action {
            data class OpenItem(val item: MediaItemVO) : Action
            object RetryLoadingRecommendedList : Action
        }
    }

    private val _stateFlow = MutableStateFlow(
        State(
            actions = emptyList(),
            playbackDetails = null,
            playItemId = savedStateHandle[KEY_PLAY_ITEM_ID]
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private val playItemId get() = stateFlow.value.playItemId

    val recommendedItemPagingDataFlow =
        stateFlow.map { it.playbackDetails }.distinctUntilChanged().flatMapLatest { playbackDetails ->
            val mediaItemLabel = MediaItemLabel.RECOMMENDED

            mediaItemRepository.clearByLabel(mediaItemLabel)   //Clear data when query changes.

            if (playbackDetails != null)
                mediaItemRepository.getMediaItemPagingDataFlow(
                    playItemId,
                    mediaItemLabel,
                    playbackDetails.subtitle
                ).map { pagingData -> pagingData.map { it.toMediaItemVO() } }
            else
                flowOf(PagingData.empty())
        }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            launch {
                stateFlow.map { it.playItemId }.flatMapLatest { playableItemId ->
                    playableItemId?.let { mediaItemRepository.getMediaItemFlowById(it) } ?: flowOf(
                        null
                    )
                }.collectLatest {
                    _stateFlow.update { state ->
                        state.copy(
                            playbackDetails = it?.toPlaybackDetailsVO(),
                        )
                    }
                }
            }
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

    fun retryLoadingRecommendedList() {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.RetryLoadingRecommendedList)
        }
    }

    fun saveState() {
        savedStateHandle[KEY_PLAY_ITEM_ID] = playItemId
    }

    fun setPlayItemId(playItemId: String?) {
        _stateFlow.update { state ->
            state.copy(playItemId = playItemId)
        }
    }
}