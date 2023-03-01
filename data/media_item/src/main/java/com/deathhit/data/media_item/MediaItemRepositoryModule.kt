package com.deathhit.data.media_item

import com.deathhit.data.media_item.data_source.MediaItemLocalDataSource
import com.deathhit.data.media_item.data_source.MediaItemRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaItemRepositoryModule {
    @Provides
    @Singleton
    internal fun provideMediaItemRepository(
        thumbnailLocalDataSource: MediaItemLocalDataSource,
        thumbnailRemoteDataSource: MediaItemRemoteDataSource
    ): MediaItemRepository =
        MediaItemRepositoryImp(thumbnailLocalDataSource, thumbnailRemoteDataSource)
}