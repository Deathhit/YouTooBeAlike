package com.deathhit.data.media_item.repository

import androidx.paging.*
import com.deathhit.core.database.model.MediaItemEntity
import com.deathhit.data.media_item.MediaItemDO
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalPagingApi::class)
internal class MediaItemRepositoryImp(
    private val mediaItemLocalDataSource: MediaItemLocalDataSource,
    private val mediaItemRemoteMediator: MediaItemRemoteMediator,
) : MediaItemRepository {
    companion object {
        private const val PAGE_SIZE = 5
    }

    override fun getThumbnailPagingDataFlow(): Flow<PagingData<MediaItemDO>> = Pager(
        PagingConfig(PAGE_SIZE),
        null,
        mediaItemRemoteMediator
    ) {
        mediaItemLocalDataSource.getMediaItemPagingSource()
    }.flow.map { pagingData -> pagingData.map { it.toDO() } }

    private fun MediaItemEntity.toDO() =
        MediaItemDO(description, sourceUrl, subtitle, thumbUrl, title)
}