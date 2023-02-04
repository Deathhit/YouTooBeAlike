package com.deathhit.data.media_item.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.deathhit.core.database.model.MediaItemEntity
import com.deathhit.core.media_api.model.Media
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource

@ExperimentalPagingApi
internal class MediaItemRemoteMediator(
    private val mediaItemLocalDataSource: MediaItemLocalDataSource,
    private val mediaItemRemoteDataSource: MediaItemRemoteDataSource
) : RemoteMediator<Int, MediaItemEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MediaItemEntity>
    ): MediatorResult {
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
                LoadType.PREPEND -> return MediatorResult.Success(true)
                // Query remoteKeyDao for the next RemoteKey.
                LoadType.APPEND ->
                    // You must explicitly check if the page key is null when
                    // appending, since null is only valid for initial load.
                    // If you receive null for APPEND, that means you have
                    // reached the end of pagination and there are no more
                    // items to load.
                    mediaItemLocalDataSource.getNextMediaItemPageKey()
                        ?: return MediatorResult.Success(
                            true
                        )
            }

            // Suspending network load via Retrofit. This doesn't need to
            // be wrapped in a withContext(Dispatcher.IO) { ... } block
            // since Retrofit's Coroutine CallAdapter dispatches on a
            // worker thread.
            val imageList = mediaItemRemoteDataSource.getMediaList(loadKey, state.config.pageSize)

            mediaItemLocalDataSource.insertMediaItemPage(
                imageList.map { it.toEntity() },
                loadType == LoadType.REFRESH,
                loadKey
            )

            MediatorResult.Success(imageList.isEmpty())
        } catch (e: Throwable) {
            MediatorResult.Error(e)
        }
    }

    private fun Media.toEntity() =
        MediaItemEntity(null, description, sourceUrl, subtitle, thumbUrl, title)
}