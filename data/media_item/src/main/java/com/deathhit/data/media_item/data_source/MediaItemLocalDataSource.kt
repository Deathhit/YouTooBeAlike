package com.deathhit.data.media_item.data_source

import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.deathhit.core.database.AppDatabase
import com.deathhit.core.database.model.MediaItemEntity
import com.deathhit.core.database.model.RemoteKeyEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaItemLocalDataSource @Inject constructor(private val appDatabase: AppDatabase) {
    companion object {
        private const val REMOTE_KEY_LABEL = "73c61cde3d515e24bad2f3239c30099f"
    }

    suspend fun getNextMediaItemPageKey() = with(appDatabase) {
        withTransaction {
            remoteKeyDao().getByLabel(REMOTE_KEY_LABEL)?.nextKey
        }
    }

    fun getMediaItemPagingSource(): PagingSource<Int, MediaItemEntity> =
        appDatabase.mediaItemDao().getPagingSource()

    suspend fun insertMediaItemPage(
        mediaItemList: List<MediaItemEntity>,
        isRefreshing: Boolean,
        pageIndex: Int,
    ) = with(appDatabase) {
        withTransaction {
            if (isRefreshing) {
                mediaItemDao().clearAll()
                remoteKeyDao().clearAll()
            }

            // Update RemoteKey for this query.
            remoteKeyDao().insertOrReplace(
                RemoteKeyEntity(REMOTE_KEY_LABEL, pageIndex + 1)
            )

            // Insert the new data into database, which invalidates the
            // current PagingData, allowing Paging to present the updates
            // in the DB.
            mediaItemDao().insertOrReplaceAll(mediaItemList)
        }
    }
}