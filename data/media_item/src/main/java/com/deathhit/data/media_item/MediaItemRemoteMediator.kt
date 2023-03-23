package com.deathhit.data.media_item

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import com.deathhit.domain.enum_type.MediaItemLabel

@ExperimentalPagingApi
internal class MediaItemRemoteMediator(
    private val exclusiveId: String?,
    private val mediaItemLocalDataSource: MediaItemLocalDataSource,
    private val mediaItemRemoteDataSource: MediaItemRemoteDataSource,
    mediaItemLabel: MediaItemLabel,
    private val subtitle: String?
) : RemoteMediator<Int, MediaItemEntity>() {
    private val label = mediaItemLabel.toLabelString()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MediaItemEntity>
    ): MediatorResult {
        return try {
            val loadKey = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKeys = getRemoteKeysClosestToCurrentPosition(label, state)

                    remoteKeys?.nextKey?.minus(1) ?: MediaItemRemoteDataSource.FIRST_PAGE
                }
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeysForFirstItem(label, state)
                    // If remoteKey is null, that means the refresh result is not in the database yet.
                    // We can return Success with 'endOfPaginationReached = false' because Paging
                    // will call this method again if RemoteKeys becomes non-null.
                    // If remoteKeys is NOT NULL but its previousKey is null, that means we've reached
                    // the end of pagination for prepend.
                    val previousKey = remoteKeys?.previousKey
                        ?: return MediatorResult.Success(remoteKeys != null)

                    previousKey
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeysForLastItem(label, state)
                    // If remoteKey is null, that means the refresh result is not in the database yet.
                    // We can return Success with 'endOfPaginationReached = false' because Paging
                    // will call this method again if RemoteKeys becomes non-null.
                    // If remoteKeys is NOT NULL but its nextKey is null, that means we've reached
                    // the end of pagination for append.
                    val nextKey =
                        remoteKeys?.nextKey ?: return MediatorResult.Success(
                            remoteKeys != null
                        )

                    nextKey
                }
            }

            // Suspending network load via Retrofit. This doesn't need to
            // be wrapped in a withContext(Dispatcher.IO) { ... } block
            // since Retrofit's Coroutine CallAdapter dispatches on a
            // worker thread.
            val itemList = mediaItemRemoteDataSource.getMediaList(
                exclusiveId,
                loadKey,
                state.config.pageSize,
                subtitle
            ).map { it.toMediaItemEntity() }

            mediaItemLocalDataSource.insertMediaItemPage(
                loadKey == MediaItemRemoteDataSource.FIRST_PAGE,
                loadType == LoadType.REFRESH,
                label,
                itemList,
                loadKey,
                state.config.pageSize
            )

            MediatorResult.Success(itemList.isEmpty())
        } catch (e: Throwable) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeysClosestToCurrentPosition(
        label: String,
        state: PagingState<Int, MediaItemEntity>
    ) = with(state) {
        anchorPosition?.let { closestItemToPosition(it) }
            ?.let {
                mediaItemLocalDataSource.getRemoteKeysByLabelAndMediaItemId(
                    label,
                    it.mediaItemId
                )
            }
    }

    private suspend fun getRemoteKeysForFirstItem(
        label: String,
        state: PagingState<Int, MediaItemEntity>
    ) = with(state) {
        firstItemOrNull()?.let {
            mediaItemLocalDataSource.getRemoteKeysByLabelAndMediaItemId(
                label,
                it.mediaItemId
            )
        }
    }

    private suspend fun getRemoteKeysForLastItem(
        label: String,
        state: PagingState<Int, MediaItemEntity>
    ) = with(state) {
        lastItemOrNull()?.let {
            mediaItemLocalDataSource.getRemoteKeysByLabelAndMediaItemId(
                label,
                it.mediaItemId
            )
        }
    }
}