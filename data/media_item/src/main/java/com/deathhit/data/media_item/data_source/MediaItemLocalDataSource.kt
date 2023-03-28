package com.deathhit.data.media_item.data_source

import androidx.paging.*
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.app_database.entity.RemoteKeysEntity
import kotlinx.coroutines.flow.Flow

internal interface MediaItemLocalDataSource {
    suspend fun clearByLabel(label: String)

    fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemEntity?>

    fun getMediaItemPagingSource(label: String): PagingSource<Int, MediaItemEntity>

    suspend fun getRemoteKeysByLabelAndMediaItemId(
        label: String,
        mediaItemId: String
    ): RemoteKeysEntity?

    suspend fun insertMediaItemPage(
        isFirstPage: Boolean,
        isRefresh: Boolean,
        label: String,
        mediaItems: List<MediaItemEntity>,
        page: Int,
        pageSize: Int
    )
}