package com.deathhit.data.media_item.config

import com.deathhit.core.media_api.model.Media
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class TestMediaItemRemoteDataSource(private val mediaItemRemoteDataSource: MediaItemRemoteDataSource) : MediaItemRemoteDataSource {
    data class State(val actions: List<Action>) {
        sealed interface Action {
            data class GetMediaList(val exclusiveId: String?, val page: Int, val pageSize: Int, val subtitle: String?) : Action
        }
    }

    private val _stateFlow = MutableStateFlow((State(actions = emptyList())))
    val stateFlow = _stateFlow.asStateFlow()

    override suspend fun getMediaList(
        exclusiveId: String?,
        page: Int,
        pageSize: Int,
        subtitle: String?
    ): List<Media> {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.GetMediaList(exclusiveId, page, pageSize, subtitle))
        }

        return mediaItemRemoteDataSource.getMediaList(exclusiveId, page, pageSize, subtitle)
    }
}