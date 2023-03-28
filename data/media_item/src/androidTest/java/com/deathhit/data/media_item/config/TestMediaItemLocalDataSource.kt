package com.deathhit.data.media_item.config

import androidx.paging.PagingSource
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.app_database.entity.RemoteKeysEntity
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import kotlinx.coroutines.flow.*

internal class TestMediaItemLocalDataSource(private val mediaItemLocalDataSource: MediaItemLocalDataSource) : MediaItemLocalDataSource {
    data class State(val actions: List<Action>) {
        sealed interface Action {
            data class ClearByLabel(val label: String) : Action
            data class GetMediaItemPagingSource(val label: String) : Action
            data class GetMediaItemFlowById(val mediaItemId: String) : Action
            data class GetRemoteKeysByLabelAndMediaItemId(
                val label: String,
                val mediaItemId: String
            ) :
                Action

            data class InsertMediaItemPage(
                val isFirstPage: Boolean,
                val isRefresh: Boolean,
                val label: String,
                val mediaItems: List<MediaItemEntity>,
                val page: Int,
                val pageSize: Int
            ) : Action
        }
    }

    private val _stateFlow = MutableStateFlow(State(actions = emptyList()))
    val stateFlow = _stateFlow.asStateFlow()

    override suspend fun clearByLabel(label: String) {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.ClearByLabel(label))
        }

        mediaItemLocalDataSource.clearByLabel(label)
    }

    override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemEntity?> {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.GetMediaItemFlowById(mediaItemId))
        }

        return mediaItemLocalDataSource.getMediaItemFlowById(mediaItemId)
    }

    override fun getMediaItemPagingSource(label: String): PagingSource<Int, MediaItemEntity> {
        _stateFlow.update { state ->
            state.copy(actions = state.actions + State.Action.GetMediaItemPagingSource(label))
        }

        return mediaItemLocalDataSource.getMediaItemPagingSource(label)
    }

    override suspend fun getRemoteKeysByLabelAndMediaItemId(
        label: String,
        mediaItemId: String
    ): RemoteKeysEntity? {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.GetRemoteKeysByLabelAndMediaItemId(
                    label,
                    mediaItemId
                )
            )
        }

        return mediaItemLocalDataSource.getRemoteKeysByLabelAndMediaItemId(label, mediaItemId)
    }

    override suspend fun insertMediaItemPage(
        isFirstPage: Boolean,
        isRefresh: Boolean,
        label: String,
        mediaItems: List<MediaItemEntity>,
        page: Int,
        pageSize: Int
    ) {
        _stateFlow.update { state ->
            state.copy(
                actions = state.actions + State.Action.InsertMediaItemPage(
                    isFirstPage,
                    isRefresh,
                    label,
                    mediaItems,
                    page,
                    pageSize
                )
            )
        }

        return mediaItemLocalDataSource.insertMediaItemPage(isFirstPage, isRefresh, label, mediaItems, page, pageSize)
    }
}