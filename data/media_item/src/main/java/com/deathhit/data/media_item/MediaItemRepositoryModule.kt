package com.deathhit.data.media_item

import androidx.paging.ExperimentalPagingApi
import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import com.deathhit.data.media_item.repository.MediaItemRemoteMediator
import com.deathhit.data.media_item.repository.MediaItemRepository
import com.deathhit.data.media_item.repository.MediaItemRepositoryImp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Module
@InstallIn(SingletonComponent::class)
object MediaItemRepositoryModule {
    @Provides
    @Singleton
    internal fun provideMediaItemRemoteMediator(
        mediaItemLocalDataSource: MediaItemLocalDataSource,
        mediaItemRemoteDataSource: MediaItemRemoteDataSource
    ) = MediaItemRemoteMediator(mediaItemLocalDataSource, mediaItemRemoteDataSource)

    @Provides
    @Singleton
    internal fun provideMediaItemRepository(
        thumbnailLocalDataSource: MediaItemLocalDataSource,
        thumbnailRemoteMediator: MediaItemRemoteMediator
    ): MediaItemRepository =
        MediaItemRepositoryImp(thumbnailLocalDataSource, thumbnailRemoteMediator)
}