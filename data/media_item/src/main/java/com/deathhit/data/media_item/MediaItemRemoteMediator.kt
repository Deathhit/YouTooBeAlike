package com.deathhit.data.media_item

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.deathhit.core.app_database.entity.MediaItemEntity
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import com.deathhit.data.media_item.model.MediaItemLabel

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
    ): MediatorResult = mediaItemLocalDataSource.loadPage(label, loadType, state) { loadKey ->
        state.firstItemOrNull()
        // Suspending network load via Retrofit. This doesn't need to
        // be wrapped in a withContext(Dispatcher.IO) { ... } block
        // since Retrofit's Coroutine CallAdapter dispatches on a
        // worker thread.
        mediaItemRemoteDataSource.getMediaList(
            exclusiveId,
            loadKey,
            state.config.pageSize,
            subtitle
        ).map { it.toMediaItemEntity() }
    }
}