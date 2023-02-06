package com.deathhit.feature.video_list.fragment.video_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.deathhit.data.media_item.repository.MediaItemRepository
import com.deathhit.data.media_progress.MediaProgressDO
import com.deathhit.data.media_progress.repository.MediaProgressRepository
import com.deathhit.feature.video_list.model.MediaItemVO
import com.deathhit.feature.video_list.model.toVO
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
        val currentPlayingMedia: MediaItemVO?
    ) {
        sealed interface Action {
            data class PrepareMedia(val item: MediaItemVO, val position: Long) : Action
            data class ShowItemClicked(val item: MediaItemVO) : Action
            object StopMedia : Action
        }
    }

    private val _stateFlow =
        MutableStateFlow(
            State(
                actions = emptyList(),
                currentPlayingMedia = null
            )
        )
    val stateFlow = _stateFlow.asStateFlow()

    val mediaItemPagingDataFlow =
        mediaItemRepository.getThumbnailPagingDataFlow()
            .map { pagingData -> pagingData.map { it.toVO() } }
            .cachedIn(viewModelScope)

    private val currentPlayingMedia get() = stateFlow.value.currentPlayingMedia

    private var prepareMediaJob: Job? = null

    fun onAction(action: State.Action) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions - action)
        }
    }

    fun prepareNewMedia(currentMediaPosition: Long, newMediaItem: MediaItemVO?) {
        if (currentPlayingMedia == newMediaItem)
            return

        saveCurrentMediaProgress(currentMediaPosition)

        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.StopMedia, currentPlayingMedia = newMediaItem)
        }

        prepareMediaJob?.cancel()
        prepareMediaJob = viewModelScope.launch {
            delay(MEDIA_SWITCHING_DELAY)

            if (newMediaItem == null)
                return@launch

            _stateFlow.update { state ->
                state.copy(
                    actions = state.actions + State.Action.PrepareMedia(
                        newMediaItem,
                        mediaProgressRepository.getMediaProgressBySourceUrl(newMediaItem.sourceUrl)?.position
                            ?: 0L
                    )
                )
            }
        }
    }

    fun saveCurrentMediaProgress(currentMediaPosition: Long) {
        currentPlayingMedia?.let {
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

    fun showItemClicked(item: MediaItemVO) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ShowItemClicked(item))
        }
    }
}