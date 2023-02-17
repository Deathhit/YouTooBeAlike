package com.deathhit.feature.media_item.fragment.media_item

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
class MediaItemListViewModel @Inject constructor(mediaItemRepository: MediaItemRepository) :
    ViewModel() {
    data class State(
        val actions: List<Action>,
        val isFirstFrameRendered: Boolean,
        val isFirstPageLoaded: Boolean,
        val playItem: MediaItemVO?,
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
                playItem = null,
                playPosition = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val mediaItemPagingDataFlow =
        mediaItemRepository.getMediaItemPagingDataFlow()
            .map { pagingData -> pagingData.map { it.toMediaItemVO() } }
            .cachedIn(viewModelScope)

    private val isFirstPageLoaded get() = stateFlow.value.isFirstPageLoaded
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

    fun prepareItem(item: MediaItemVO?) {
        if (item == playItem)
            return

        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.PrepareItem(item),
                isFirstFrameRendered = false,
                playItem = item
            )
        }
    }

    fun scrollToTopOnFirstPageLoaded() {
        if (isFirstPageLoaded)
            return

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ScrollToTop, isFirstPageLoaded = true)
        }
    }

    fun setPlayPosition(playPosition: Int?) {
        if (playPosition == this.playPosition)
            return

        _stateFlow.update { state ->
            state.copy(playPosition = playPosition)
        }

        if (playPosition == null)
            prepareItem(null)
    }
}