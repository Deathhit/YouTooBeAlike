package com.deathhit.data.media_item.config

import androidx.paging.PagingSource
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.app_database.entity.RemoteKeysEntity
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import kotlinx.coroutines.flow.Flow

internal class TestMediaItemLocalDataSource(private val mediaItemLocalDataSource: MediaItemLocalDataSource) :
    MediaItemLocalDataSource {
    sealed interface Action {
        data class ClearByLabel(val label: String) : Action
        data class GetMediaItemPagingSource(val label: String) : Action
        data class GetMediaItemFlowById(val mediaItemId: String) : Action
        data class GetRemoteKeysByLabelAndMediaItemId(val label: String, val mediaItemId: String) :
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

    var actions = emptyList<Action>()

    override suspend fun clearByLabel(label: String) =
        mediaItemLocalDataSource.clearByLabel(label).also {
            actions = actions + Action.ClearByLabel(label)
        }

    override fun getMediaItemPagingSource(label: String): PagingSource<Int, MediaItemEntity> =
        mediaItemLocalDataSource.getMediaItemPagingSource(label).also {
            actions = actions + Action.GetMediaItemPagingSource(label)
        }

    override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemEntity?> =
        mediaItemLocalDataSource.getMediaItemFlowById(mediaItemId).also {
            actions = actions + Action.GetMediaItemFlowById(mediaItemId)
        }

    override suspend fun getRemoteKeysByLabelAndMediaItemId(
        label: String,
        mediaItemId: String
    ): RemoteKeysEntity? = mediaItemLocalDataSource.getRemoteKeysByLabelAndMediaItemId(label, mediaItemId).also {
        actions = actions + Action.GetRemoteKeysByLabelAndMediaItemId(label, mediaItemId)
    }

    override suspend fun insertMediaItemPage(
        isFirstPage: Boolean,
        isRefresh: Boolean,
        label: String,
        mediaItems: List<MediaItemEntity>,
        page: Int,
        pageSize: Int
    ) = mediaItemLocalDataSource.insertMediaItemPage(isFirstPage, isRefresh, label, mediaItems, page, pageSize).also {
        actions = actions + Action.InsertMediaItemPage(isFirstPage, isRefresh, label, mediaItems, page, pageSize)
    }
}