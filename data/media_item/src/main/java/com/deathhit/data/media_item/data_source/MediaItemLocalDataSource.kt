package com.deathhit.data.media_item.data_source

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.deathhit.core.database.AppDatabase
import com.deathhit.core.database.entity.MediaItemEntity
import com.deathhit.core.database.entity.RemoteKeyEntity
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
internal class MediaItemLocalDataSource @Inject constructor(private val appDatabase: AppDatabase) {
    private val mediaItemDao = appDatabase.mediaItemDao()
    private val remoteKeyDao = appDatabase.remoteKeyDao()

    suspend fun clearByLabel(label: String) =
        mediaItemDao.clearByLabel(label)

    fun getMediaItemPagingSource(label: String): PagingSource<Int, MediaItemEntity> =
        mediaItemDao.getPagingSource(label)

    fun getMediaItemFlowById(mediaItemId: String) = mediaItemDao.getFlowById(mediaItemId)

    suspend fun loadPage(
        label: String,
        loadType: LoadType,
        remotePageFetcher: suspend (loadKey: Int?) -> List<MediaItemEntity>
    ): RemoteMediator.MediatorResult {
        return try {
            // The network load method takes an optional String
            // parameter. For every page after the first, pass the String
            // token returned from the previous page to let it continue
            // from where it left off. For REFRESH, pass to load the
            // first page.
            val loadKey = when (loadType) {
                LoadType.REFRESH -> MediaItemRemoteDataSource.DEFAULT_PAGE
                // In this example, you never need to prepend, since REFRESH
                // will always load the first page in the list. Immediately
                // return, reporting end of pagination.
                LoadType.PREPEND -> return RemoteMediator.MediatorResult.Success(
                    true
                )
                // Query remoteKeyDao for the next RemoteKey.
                LoadType.APPEND ->
                    // You must explicitly check if the page key is null when
                    // appending, since null is only valid for initial load.
                    // If you receive null for APPEND, that means you have
                    // reached the end of pagination and there are no more
                    // items to load.
                    appDatabase.withTransaction { remoteKeyDao.getByLabel(label)?.nextKey }
                        ?: return RemoteMediator.MediatorResult.Success(
                            true
                        )
            }

            val itemList = remotePageFetcher.invoke(loadKey)

            appDatabase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    mediaItemDao.clearByLabel(label)
                    remoteKeyDao.clearAll(label)
                }

                // Update RemoteKey for this query.
                remoteKeyDao.upsert(RemoteKeyEntity(label, loadKey + 1))

                // Insert the new data into database, which invalidates the
                // current PagingData, allowing Paging to present the updates
                // in the DB.
                mediaItemDao.upsert(itemList)
            }

            RemoteMediator.MediatorResult.Success(itemList.isEmpty())
        } catch (e: Throwable) {
            RemoteMediator.MediatorResult.Error(e)
        }
    }
}