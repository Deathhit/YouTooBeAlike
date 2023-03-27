package com.deathhit.domain.test

import com.deathhit.domain.MediaProgressRepository
import com.deathhit.domain.model.MediaProgressDO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeMediaProgressRepository: MediaProgressRepository {
    data class State(val actions: List<Action>) {
        sealed interface Action {
            data class GetMediaProgressByMediaItemId(val mediaItemId: String) : Action
            data class SetMediaProgress(val mediaProgressDO: MediaProgressDO) : Action
        }
    }

    private val _stateFlow = MutableStateFlow(State(actions = emptyList()))
    val stateFlow = _stateFlow.asStateFlow()

    var funcGetMediaProgressByMediaItemId: (suspend (mediaItemId: String) -> MediaProgressDO?)? = null
    var funcSetMediaProgress: (suspend (mediaProgressDO: MediaProgressDO) -> Unit)? = null

    override suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressDO? {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.GetMediaProgressByMediaItemId(mediaItemId))
        }

        return funcGetMediaProgressByMediaItemId?.invoke(mediaItemId)
    }

    override suspend fun setMediaProgress(mediaProgressDO: MediaProgressDO) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.SetMediaProgress(mediaProgressDO))
        }

        funcSetMediaProgress?.invoke(mediaProgressDO)
    }
}