package com.deathhit.data.media_item.data_source

import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.deathhit.core.database.AppDatabase
import com.deathhit.core.database.model.MediaItemEntity
import com.deathhit.core.database.model.RemoteKeyEntity
import com.deathhit.data.media_item.model.MediaItemSourceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaItemLocalDataSource @Inject constructor(private val appDatabase: AppDatabase) {
    private val mediaItemDao = appDatabase.mediaItemDao()
    private val remoteKeyDao = appDatabase.remoteKeyDao()

    suspend fun clearAll(mediaItemSourceType: MediaItemSourceType) =
        mediaItemDao.clearAll(mediaItemSourceType.columnValue)

    fun getMediaItemPagingSource(mediaItemSourceType: MediaItemSourceType): PagingSource<Int, MediaItemEntity> =
        mediaItemDao.getPagingSource(mediaItemSourceType.columnValue)

    fun getMediaItemFlowById(mediaItemId: String) = mediaItemDao.getFlowById(mediaItemId)

    suspend fun getNextMediaItemPageKey(mediaItemSourceType: MediaItemSourceType) =
        with(appDatabase) {
            withTransaction {
                remoteKeyDao.getByLabel(mediaItemSourceType.remoteKeyLabel)?.nextKey
            }
        }

    suspend fun insertMediaItemPage(
        mediaItemList: List<MediaItemEntity>,
        mediaItemSourceType: MediaItemSourceType,
        isRefreshing: Boolean,
        pageIndex: Int,
    ) = with(appDatabase) {
        withTransaction {
            if (isRefreshing) {
                mediaItemDao.clearAll(mediaItemSourceType.columnValue)
                remoteKeyDao().clearAll(mediaItemSourceType.remoteKeyLabel)
            }

            // Update RemoteKey for this query.
            remoteKeyDao.insertOrReplace(
                RemoteKeyEntity(mediaItemSourceType.remoteKeyLabel, pageIndex + 1)
            )

            // Insert the new data into database, which invalidates the
            // current PagingData, allowing Paging to present the updates
            // in the DB.
            mediaItemDao.upsert(mediaItemList)
        }
    }
}