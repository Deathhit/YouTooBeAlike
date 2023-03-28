package com.deathhit.core.media_api.test

import com.deathhit.core.media_api.MediaApiService
import com.deathhit.core.media_api.model.Media
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeMediaApiService : MediaApiService {
    data class State(val actions: List<Action>) {
        sealed interface Action {
            data class GetMediaList(val exclusiveId: String?, val page: Int, val pageSize: Int, val subtitle: String?) : Action
        }
    }

    private val _stateFlow = MutableStateFlow(State(actions = emptyList()))
    val stateFlow = _stateFlow.asStateFlow()

    var funcGetMediaList: ((exclusiveId: String?, page: Int, pageSize: Int, subtitle: String?) -> List<Media>)? = null

    override suspend fun getMediaList(
        exclusiveId: String?,
        page: Int,
        pageSize: Int,
        subtitle: String?
    ): List<Media> {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.GetMediaList(exclusiveId, page, pageSize, subtitle))
        }

        return funcGetMediaList?.invoke(exclusiveId, page, pageSize, subtitle) ?: emptyList()
    }
}