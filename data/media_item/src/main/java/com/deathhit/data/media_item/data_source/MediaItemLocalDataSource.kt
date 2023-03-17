package com.deathhit.data.media_item.data_source

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import com.deathhit.core.app_database.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalPagingApi::class)
internal interface MediaItemLocalDataSource {
    suspend fun clearByLabel(label: String)

    fun getMediaItemPagingSource(label: String): PagingSource<Int, MediaItemEntity>

    fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemEntity?>

    suspend fun loadPage(
        label: String,
        loadType: LoadType,
        remotePageFetcher: suspend (loadKey: Int?) -> List<MediaItemEntity>
    ): RemoteMediator.MediatorResult
}