package com.deathhit.video_list_example.fragment.video_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathhit.domain.repository.video.VideoRepository
import com.deathhit.lib_sign_able.SignAble
import com.deathhit.video_list_example.extensions.toVO
import com.deathhit.video_list_example.model.VideoVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class VideoListViewModel @Inject constructor(private val videoRepository: VideoRepository) :
    ViewModel() {
    companion object {
        private const val DELAY_PLAY_AT_POSITION = 1500L
        private const val POSITION_INVALID = -1
    }

    data class State(
        val argPlayPosition: Int,
        val argVideoPositionMap: Map<Int, Long>,
        val eventOnClickItem: SignAble<VideoVO> = SignAble(),
        val eventPlayAtPosition: SignAble<Int> = SignAble(),
        val eventScrollToNextItem: SignAble<Unit> = SignAble(),
        val statusVideoList: SignAble<List<VideoVO>> = SignAble()
    )

    private val _stateFlow =
        MutableStateFlow(State(POSITION_INVALID, emptyMap()))
    val stateFlow = _stateFlow.asStateFlow()

    private var setPlayPositionJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val list = videoRepository.getVideoList().map { it.toVO() }

            launch(Dispatchers.Main) {
                _stateFlow.update { state ->
                    state.copy(statusVideoList = SignAble(list))
                }
            }
        }
    }

    fun onClickItem(item: VideoVO) {
        _stateFlow.update { state ->
            state.copy(eventOnClickItem = SignAble(item))
        }
    }

    fun saveVideoPosition(itemPosition: Int, videoPosition: Long) {
        _stateFlow.update { state ->
            state.copy(
                argVideoPositionMap = state.argVideoPositionMap.toMutableMap()
                    .apply { this[itemPosition] = videoPosition })
        }
    }

    fun scrollToNextItem() {
        _stateFlow.update { state ->
            state.copy(eventScrollToNextItem = SignAble(Unit))
        }
    }

    fun setPlayPosition(newPlayPosition: Int) {
        with(stateFlow.value) {
            if (newPlayPosition == argPlayPosition)
                return

            _stateFlow.update { state ->
                state.copy(argPlayPosition = newPlayPosition, eventPlayAtPosition = SignAble(POSITION_INVALID))
            }

            setPlayPositionJob?.cancel()
            setPlayPositionJob = viewModelScope.launch {
                delay(DELAY_PLAY_AT_POSITION)
                _stateFlow.update { state ->
                    state.copy(eventPlayAtPosition = SignAble(newPlayPosition))
                }
            }
        }
    }
}