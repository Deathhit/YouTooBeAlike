package com.deathhit.data.media_item.data_source

import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.media_api.MediaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataSourceModule {
    @Provides
    @Singleton
    fun provideMediaItemLocalDataSource(appDatabase: AppDatabase): MediaItemLocalDataSource =
        MediaItemLocalDataSourceImp(appDatabase)

    @Provides
    @Singleton
    fun provideMediaItemRemoteDataSource(mediaApiService: MediaApiService): MediaItemRemoteDataSource =
        MediaItemRemoteDataSourceImp(mediaApiService)
}