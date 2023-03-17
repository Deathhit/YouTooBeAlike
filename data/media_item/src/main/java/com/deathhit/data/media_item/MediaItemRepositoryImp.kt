package com.deathhit.data.media_item

import androidx.paging.*
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import com.deathhit.data.media_item.model.MediaItemDO
import com.deathhit.data.media_item.model.MediaItemLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalPagingApi::class)
internal class MediaItemRepositoryImp(
    private val mediaItemLocalDataSource: MediaItemLocalDataSource,
    private val mediaItemRemoteDataSource: MediaItemRemoteDataSource,
) : MediaItemRepository {
    companion object {
        private const val PAGE_SIZE = 5 //PAGE_SIZE should be much larger. This is for demo purpose.
    }

    override suspend fun clearByLabel(mediaItemLabel: MediaItemLabel) =
        mediaItemLocalDataSource.clearByLabel(mediaItemLabel.toLabelString())

    override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemDO?> =
        mediaItemLocalDataSource.getMediaItemFlowById(mediaItemId).map { it?.toMediaItemDO() }

    override fun getMediaItemPagingDataFlow(
        exclusiveId: String?,
        mediaItemLabel: MediaItemLabel,
        subtitle: String?
    ): Flow<PagingData<MediaItemDO>> = Pager(
        PagingConfig(PAGE_SIZE),
        null,
        MediaItemRemoteMediator(
            exclusiveId,
            mediaItemLocalDataSource,
            mediaItemRemoteDataSource,
            mediaItemLabel,
            subtitle
        )
    ) {
        mediaItemLocalDataSource.getMediaItemPagingSource(mediaItemLabel.toLabelString())
    }.flow.map { pagingData -> pagingData.map { it.toMediaItemDO() } }
}