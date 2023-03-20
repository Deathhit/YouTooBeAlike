package com.deathhit.data.media_item.config

import com.deathhit.core.app_database.AppDatabase
import com.deathhit.core.media_api.MediaApiService
import com.deathhit.data.media_item.data_source.*
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataSourceModule::class]
)
internal object TestDataSourceModule {
    @Provides
    @Singleton
    fun provideMediaItemLocalDataSource(testMediaItemLocalDataSource: TestMediaItemLocalDataSource): MediaItemLocalDataSource =
        testMediaItemLocalDataSource

    @Provides
    @Singleton
    fun provideMediaItemLocalDataSourceImp(appDatabase: AppDatabase): MediaItemLocalDataSourceImp =
        MediaItemLocalDataSourceImp(appDatabase)

    @Provides
    @Singleton
    fun provideMediaItemRemoteDataSource(testMediaItemRemoteDataSource: TestMediaItemRemoteDataSource): MediaItemRemoteDataSource =
        testMediaItemRemoteDataSource

    @Provides
    @Singleton
    fun provideMediaItemRemoteDataSourceImp(mediaApiService: MediaApiService): MediaItemRemoteDataSourceImp =
        MediaItemRemoteDataSourceImp(mediaApiService)

    @Provides
    @Singleton
    fun provideTestMediaItemLocalDataSource(mediaItemLocalDataSourceImp: MediaItemLocalDataSourceImp): TestMediaItemLocalDataSource =
        TestMediaItemLocalDataSource(mediaItemLocalDataSourceImp)

    @Provides
    @Singleton
    fun provideTestMediaItemRemoteDataSource(mediaItemRemoteDataSourceImp: MediaItemRemoteDataSourceImp): TestMediaItemRemoteDataSource =
        TestMediaItemRemoteDataSource(mediaItemRemoteDataSourceImp)
}