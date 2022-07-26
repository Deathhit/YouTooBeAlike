package com.deathhit.video_list_example.fragment.video_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.domain.repository.video.VideoRepository
import com.deathhit.video_list_example.model.VideoVO
import com.deathhit.video_list_example.model.toVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class VideoListViewModel @Inject constructor(private val videoRepository: VideoRepository) :
    ViewModel() {
    companion object {
        private const val VALUE_DELAY_PLAY_ITEM = 1500L
        private const val VALUE_POSITION_INVALID = -1
    }

    sealed class Event {
        data class PlayItemAtPosition(val position: Int) : Event()
        object ScrollToNextItem : Event()
        data class ShowItemClicked(val item: VideoVO) : Event()
    }

    data class State(
        val itemList: List<VideoVO>,
        val itemPositionMap: Map<Int, Long>,
        val playPosition: Int
    )

    private val _eventChannel = Channel<Event>()
    val eventFlow = _eventChannel.receiveAsFlow()

    private val _stateFlow =
        MutableStateFlow(State(emptyList(), emptyMap(), VALUE_POSITION_INVALID))
    val stateFlow = _stateFlow.asStateFlow()

    private var setPlayPositionJob: Job? = null

    init {
        viewModelScope.launch {
            _stateFlow.update { state ->
                state.copy(itemList = videoRepository.getVideoList().map { it.toVO() })
            }
        }
    }

    fun onClickItem(item: VideoVO) {
        viewModelScope.launch {
            _eventChannel.send(Event.ShowItemClicked(item))
        }
    }

    fun onPlaybackEnded() {
        viewModelScope.launch {
            _eventChannel.send(Event.ScrollToNextItem)
        }
    }

    fun onSaveItemPosition(itemPosition: Long, playPosition: Int) {
        _stateFlow.update { state ->
            state.copy(
                itemPositionMap = state.itemPositionMap.toMutableMap()
                    .apply { this[playPosition] = itemPosition })
        }
    }

    fun onScrolled(newPlayPosition: Int) {
        with(stateFlow.value) {
            if (newPlayPosition == playPosition)
                return

            _stateFlow.update { state ->
                state.copy(playPosition = newPlayPosition)
            }

            viewModelScope.launch {
                _eventChannel.send(Event.PlayItemAtPosition(VALUE_POSITION_INVALID))
            }

            setPlayPositionJob?.cancel()
            setPlayPositionJob = viewModelScope.launch {
                delay(VALUE_DELAY_PLAY_ITEM)
                viewModelScope.launch {
                    _eventChannel.send(Event.PlayItemAtPosition(newPlayPosition))
                }
            }
        }
    }
}