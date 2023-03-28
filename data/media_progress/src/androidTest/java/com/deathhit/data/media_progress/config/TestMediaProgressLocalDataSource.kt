package com.deathhit.data.media_progress.config

import com.deathhit.core.app_database.entity.MediaProgressEntity
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource
import hilt_aggregated_deps._com_deathhit_data_media_progress_data_source_HiltWrapper_DataSourceModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class TestMediaProgressLocalDataSource(private val mediaProgressLocalDataSource: MediaProgressLocalDataSource) :
    MediaProgressLocalDataSource {
    data class State(val actions: List<Action>) {
        sealed interface Action {
            data class GetMediaProgressByMediaItemId(val mediaItemId: String) : Action
            data class SetMediaProgress(val mediaProgressEntity: MediaProgressEntity) : Action
        }
    }

    private val _stateFlow = MutableStateFlow(State(actions = emptyList()))
    val stateFlow = _stateFlow.asStateFlow()

    override suspend fun getMediaProgressByMediaItemId(mediaItemId: String): MediaProgressEntity? {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.GetMediaProgressByMediaItemId(mediaItemId))
        }

        return mediaProgressLocalDataSource.getMediaProgressByMediaItemId(mediaItemId)
    }

    override suspend fun setMediaProgress(mediaProgressEntity: MediaProgressEntity) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.SetMediaProgress(mediaProgressEntity))
        }

        mediaProgressLocalDataSource.setMediaProgress(mediaProgressEntity)
    }
}