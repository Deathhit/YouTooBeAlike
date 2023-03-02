package com.deathhit.data.media_item.data_source

import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.deathhit.core.database.AppDatabase
import com.deathhit.core.database.model.MediaItemEntity
import com.deathhit.core.database.model.RemoteKeyEntity
import com.deathhit.data.media_item.model.MediaItemLabel
import com.deathhit.data.media_item.toLabel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaItemLocalDataSource @Inject constructor(private val appDatabase: AppDatabase) {
    private val mediaItemDao = appDatabase.mediaItemDao()
    private val remoteKeyDao = appDatabase.remoteKeyDao()

    suspend fun clearAll(mediaItemLabel: MediaItemLabel) =
        mediaItemDao.clearAll(mediaItemLabel.toLabel())

    fun getMediaItemPagingSource(mediaItemLabel: MediaItemLabel): PagingSource<Int, MediaItemEntity> =
        mediaItemDao.getPagingSource(mediaItemLabel.toLabel())

    fun getMediaItemFlowById(mediaItemId: String) = mediaItemDao.getFlowById(mediaItemId)

    suspend fun getNextMediaItemPageKey(mediaItemLabel: MediaItemLabel) =
        with(appDatabase) {
            withTransaction {
                remoteKeyDao.getByLabel(mediaItemLabel.toLabel())?.nextKey
            }
        }

    suspend fun insertMediaItemPage(
        mediaItemList: List<MediaItemEntity>,
        mediaItemLabel: MediaItemLabel,
        isRefreshing: Boolean,
        pageIndex: Int,
    ) = with(appDatabase) {
        withTransaction {
            val label = mediaItemLabel.toLabel()

            if (isRefreshing) {
                mediaItemDao.clearAll(label)
                remoteKeyDao.clearAll(label)
            }

            // Update RemoteKey for this query.
            remoteKeyDao.upsert(RemoteKeyEntity(label, pageIndex + 1))

            // Insert the new data into database, which invalidates the
            // current PagingData, allowing Paging to present the updates
            // in the DB.
            mediaItemDao.upsert(mediaItemList)
        }
    }
}