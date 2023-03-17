package com.deathhit.data.media_item.data_source

import androidx.paging.*
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.app_database.entity.RemoteKeysEntity
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalPagingApi::class)
internal interface MediaItemLocalDataSource {
    suspend fun clearByLabel(label: String)

    fun getMediaItemPagingSource(label: String): PagingSource<Int, MediaItemEntity>

    fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemEntity?>

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