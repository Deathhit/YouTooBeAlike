package com.deathhit.feature.media_item.fragment.media_item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.repository.MediaItemRepository
import com.deathhit.feature.media_item.model.ItemVO
import com.deathhit.feature.media_item.model.toItemVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MediaItemListViewModel @Inject constructor(mediaItemRepository: MediaItemRepository) : ViewModel() {
    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val playItem: ItemVO?,
        val playPosition: Int?
    ) {
        sealed interface Action {
            data class ClickItem(val item: ItemVO) : Action
            data class PrepareItem(val item: ItemVO?) : Action
            object StopPlayer : Action
        }
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                isFirstFrameRendered = false,
                playItem = null,
                playPosition = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val videoPagingDataFlow =
        mediaItemRepository.getThumbnailPagingDataFlow()
            .map { pagingData -> pagingData.map { it.toItemVO() } }
            .cachedIn(viewModelScope)

    private val playItem get() = stateFlow.value.playItem
    private val playPosition get() = stateFlow.value.playPosition

    fun clickItem(item: ItemVO) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ClickItem(item))
        }
    }

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

    fun preparePlayItem(playItem: ItemVO?) {
        if (playItem == this.playItem)
            return

        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.PrepareItem(playItem),
                playItem = playItem
            )
        }
    }

    fun setPlayPosition(playPosition: Int?) {
        if (playPosition == this.playPosition)
            return

        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.StopPlayer,
                isFirstFrameRendered = false,
                playPosition = playPosition
            )
        }

        preparePlayItem(null)
    }
}