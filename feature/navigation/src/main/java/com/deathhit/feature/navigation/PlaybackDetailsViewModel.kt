package com.deathhit.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.MediaItemSourceType
import com.deathhit.data.media_item.repository.MediaItemRepository
import com.deathhit.feature.media_item.model.MediaItemVO
import com.deathhit.feature.media_item.model.toMediaItemVO
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
        private const val KEY_PLAYABLE_ITEM_ID = "$TAG.KEY_PLAYABLE_ITEM_ID"
    }

    data class State(
        val actions: List<Action>,
        val playableItem: MediaItemVO?,
        val playableItemId: String?
    ) {
        sealed interface Action {
            data class OpenPlayableItem(val item: MediaItemVO) : Action
        }
    }

    private val _stateFlow = MutableStateFlow(
        State(
            actions = emptyList(),
            playableItem = null,
            playableItemId = savedStateHandle[KEY_PLAYABLE_ITEM_ID]
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private val playableItemId get() = stateFlow.value.playableItemId

    val recommendedItemPagingDataFlow =
        stateFlow.map { it.playableItem }.distinctUntilChanged().flatMapLatest { playableItem ->
            val mediaItemSourceType = MediaItemSourceType.RECOMMENDED

            mediaItemRepository.clearAll(mediaItemSourceType)   //Clear data when query changes.

            if (playableItem != null)
                mediaItemRepository.getMediaItemPagingDataFlow(
                    playableItem.id,
                    mediaItemSourceType,
                    playableItem.subtitle
                ).map { pagingData -> pagingData.map { it.toMediaItemVO() } }
            else
                flowOf(PagingData.empty())
        }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            launch {
                stateFlow.map { it.playableItemId }.flatMapLatest { playableItemId ->
                    playableItemId?.let { mediaItemRepository.getMediaItemFlowById(it) } ?: flowOf(
                        null
                    )
                }.collectLatest {
                    _stateFlow.update { state ->
                        state.copy(playableItem = it?.toMediaItemVO())
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

    fun openPlayableItem(item: MediaItemVO) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.OpenPlayableItem(item))
        }
    }

    fun saveState() {
        savedStateHandle[KEY_PLAYABLE_ITEM_ID] = playableItemId
    }

    fun setPlayableItemId(playableItemId: String?) {
        if (playableItemId == this.playableItemId)
            return

        _stateFlow.update { state ->
            state.copy(playableItemId = playableItemId)
        }
    }
}