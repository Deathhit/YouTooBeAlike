package com.deathhit.data.media_item.data_source

import androidx.paging.*
import androidx.room.withTransaction
import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.core.app_database.entity.RemoteKeysEntity
import com.deathhit.core.media_api.MediaApiService
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalPagingApi::class)
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

    override suspend fun loadPage(
        label: String,
        loadType: LoadType,
        pagingState: PagingState<Int, MediaItemEntity>,
        remotePageFetcher: suspend (loadKey: Int?) -> List<MediaItemEntity>
    ): RemoteMediator.MediatorResult {
        return try {
            val loadKey = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKeys = getRemoteKeysClosestToCurrentPosition(label, pagingState)

                    remoteKeys?.nextKey?.minus(1) ?: MediaApiService.DEFAULT_PAGE
                }
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeysForFirstItem(label, pagingState)
                    // If remoteKey is null, that means the refresh result is not in the database yet.
                    // We can return Success with 'endOfPaginationReached = false' because Paging
                    // will call this method again if RemoteKeys becomes non-null.
                    // If remoteKeys is NOT NULL but its previousKey is null, that means we've reached
                    // the end of pagination for prepend.
                    val previousKey = remoteKeys?.previousKey
                        ?: return RemoteMediator.MediatorResult.Success(remoteKeys != null)

                    previousKey
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeysForLastItem(label, pagingState)
                    // If remoteKey is null, that means the refresh result is not in the database yet.
                    // We can return Success with 'endOfPaginationReached = false' because Paging
                    // will call this method again if RemoteKeys becomes non-null.
                    // If remoteKeys is NOT NULL but its nextKey is null, that means we've reached
                    // the end of pagination for append.
                    val nextKey =
                        remoteKeys?.nextKey ?: return RemoteMediator.MediatorResult.Success(
                            remoteKeys != null
                        )

                    nextKey
                }
            }

            val itemList = remotePageFetcher.invoke(loadKey)
            val endOfPaginationReached = itemList.isEmpty()

            appDatabase.withTransaction {
                //Clear tables on refresh
                if (loadType == LoadType.REFRESH) {
                    mediaItemDao.clearByLabel(label)
                    remoteKeysDao.clearByLabel(label)
                }

                val previousKey = if (loadKey == MediaApiService.DEFAULT_PAGE) null else loadKey - 1
                val nextKey = if (endOfPaginationReached) null else loadKey + 1

                // Update RemoteKeys for the label.
                remoteKeysDao.upsert(itemList.map {
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
                mediaItemDao.upsert(itemList.mapIndexed { index, mediaItemEntity ->
                    mediaItemEntity.copy(
                        label = label,
                        remoteOrder = loadKey * pagingState.config.pageSize + index
                    )
                })
            }

            RemoteMediator.MediatorResult.Success(endOfPaginationReached)
        } catch (e: Throwable) {
            RemoteMediator.MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeysClosestToCurrentPosition(
        label: String,
        pagingState: PagingState<Int, MediaItemEntity>
    ) = with(pagingState) {
        anchorPosition?.let { closestItemToPosition(it) }
            ?.let { remoteKeysDao.getByLabelAndMediaItemId(label, it.mediaItemId) }
    }

    private suspend fun getRemoteKeysForFirstItem(
        label: String,
        pagingState: PagingState<Int, MediaItemEntity>
    ) = with(pagingState) {
        firstItemOrNull()?.let { remoteKeysDao.getByLabelAndMediaItemId(label, it.mediaItemId) }
    }

    private suspend fun getRemoteKeysForLastItem(
        label: String,
        pagingState: PagingState<Int, MediaItemEntity>
    ) = with(pagingState) {
        lastItemOrNull()?.let { remoteKeysDao.getByLabelAndMediaItemId(label, it.mediaItemId) }
    }
}