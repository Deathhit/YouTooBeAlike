package com.deathhit.video_list_example.fragment.video_list

import android.net.Uri
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
        private const val DELAY_PLAY_AT_POS = 1500L
        private const val POS_INVALID = -1
    }

    data class State(
        val argPlayPos: Int,
        val argPositionMap: Map<String, Long>,
        val eventPlayAtPos: SignAble<Int> = SignAble(),
        val eventOnClickVideo: SignAble<Uri> = SignAble(),
        val statusVideoList: SignAble<List<VideoVO>> = SignAble()
    )

    private val _stateFlow =
        MutableStateFlow(State(POS_INVALID, emptyMap()))
    val stateFlow = _stateFlow.asStateFlow()

    private var setPlayPosJob: Job? = null

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

    fun saveVideoPosition(sourceUrl: String, videoPosition: Long) {
        _stateFlow.update { state ->
            state.copy(
                argPositionMap = state.argPositionMap.toMutableMap()
                    .apply { this[sourceUrl] = videoPosition })
        }
    }

    fun setPlayPos(newPos: Int) {
        with(stateFlow.value) {
            if (newPos == argPlayPos)
                return

            _stateFlow.update { state ->
                state.copy(argPlayPos = newPos, eventPlayAtPos = SignAble(POS_INVALID))
            }

            setPlayPosJob?.cancel()
            setPlayPosJob = viewModelScope.launch {
                delay(DELAY_PLAY_AT_POS)
                _stateFlow.update { state ->
                    state.copy(eventPlayAtPos = SignAble(newPos))
                }
            }
        }
    }
}