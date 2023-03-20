package com.deathhit.data.media_item.data_source

import androidx.paging.*
import androidx.room.withTransaction
import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.app_database.entity.RemoteKeysEntity
import kotlinx.coroutines.flow.Flow

internal class MediaItemLocalDataSourceImp(private val appDatabase: AppDatabase) :
    MediaItemLocalDataSource {
    private val mediaItemDao = appDatabase.mediaItemDao()
    private val remoteKeysDao = appDatabase.remoteKeysDao()

    override suspend fun clearByLabel(label: String) =
        mediaItemDao.clearByLabel(label)

    override fun getMediaItemPagingSource(label: String): PagingSource<Int, MediaItemEntity> =
        mediaItemDao.getPagingSource(label)

    override fun getMediaItemFlowById(mediaItemId: String): Flow<MediaItemEntity?> =
        mediaItemDao.getFlowById(mediaItemId)

    override suspend fun getRemoteKeysByLabelAndMediaItemId(
        label: String,
        mediaItemId: String
    ): RemoteKeysEntity? = remoteKeysDao.getByLabelAndMediaItemId(label, mediaItemId)

    override suspend fun insertMediaItemPage(
        isFirstPage: Boolean,
        isRefresh: Boolean,
        label: String,
        mediaItems: List<MediaItemEntity>,
        page: Int,
        pageSize: Int
    ) = appDatabase.withTransaction {
        //Clear tables on refresh
        if (isRefresh) {
            mediaItemDao.clearByLabel(label)
            remoteKeysDao.clearByLabel(label)
        }

        val nextKey = page + 1
        val previousKey = if (isFirstPage) null else page - 1

        // Update RemoteKeys for the label.
        remoteKeysDao.upsert(mediaItems.map {
            RemoteKeysEntity(
                label = label,
                mediaItemId = it.mediaItemId,
                nextKey = nextKey,
                previousKey = previousKey
            )
        })

        // Insert the new data into database, which invalidates the
        // current PagingData, allowing Paging to present the updates
        // in the DB.
        mediaItemDao.upsert(mediaItems.mapIndexed { index, mediaItemEntity ->
            mediaItemEntity.copy(
                label = label,
                remoteOrder = page * pageSize + index
            )
        })
    }
}