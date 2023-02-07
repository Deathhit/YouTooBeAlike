package com.deathhit.feature.video_list.fragment.video_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.repository.MediaItemRepository
import com.deathhit.data.media_progress.MediaProgressDO
import com.deathhit.data.media_progress.repository.MediaProgressRepository
import com.deathhit.feature.video_list.model.VideoVO
import com.deathhit.feature.video_list.model.toVideoVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoListViewModel @Inject constructor(
    mediaItemRepository: MediaItemRepository,
    private val mediaProgressRepository: MediaProgressRepository
) : ViewModel() {
    companion object {
        private const val MEDIA_SWITCHING_DELAY = 500L
    }

    data class State(
        val actions: List<Action>,
        val playItem: VideoVO?
    ) {
        sealed interface Action {
            data class PrepareMedia(val item: VideoVO, val position: Long) : Action
            data class ShowItemClicked(val item: VideoVO) : Action
            object StopMedia : Action
        }
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                playItem = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val mediaItemPagingDataFlow =
        mediaItemRepository.getThumbnailPagingDataFlow()
            .map { pagingData -> pagingData.map { it.toVideoVO() } }
            .cachedIn(viewModelScope)

    private val playItem get() = stateFlow.value.playItem

    private var prepareMediaJob: Job? = null

    fun clearPlayItem() {
        _stateFlow.update { state ->
            state.copy(playItem = null)
        }
    }

    fun onAction(action: State.Action) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions - action)
        }
    }

    fun prepareNewPlayItem(currentMediaPosition: Long, newPlayItem: VideoVO?) {
        if (playItem == newPlayItem)
            return

        saveCurrentMediaProgress(currentMediaPosition)

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.StopMedia, playItem = newPlayItem)
        }

        prepareMediaJob?.cancel()
        prepareMediaJob = viewModelScope.launch {
            delay(MEDIA_SWITCHING_DELAY)

            if (newPlayItem == null)
                return@launch

            _stateFlow.update { state ->
                state.copy(
                    actions = state.actions + State.Action.PrepareMedia(
                        newPlayItem,
                        mediaProgressRepository.getMediaProgressBySourceUrl(newPlayItem.sourceUrl)?.position
                            ?: 0L
                    )
                )
            }
        }
    }

    fun saveCurrentMediaProgress(currentMediaPosition: Long) {
        playItem?.let {
            viewModelScope.launch {
                mediaProgressRepository.setMediaProgress(
                    MediaProgressDO(
                        currentMediaPosition,
                        it.sourceUrl
                    )
                )
            }
        }
    }

    fun showItemClicked(item: VideoVO) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ShowItemClicked(item))
        }
    }
}