package com.deathhit.data.media_progress.config

import com.deathhit.core.app_database.AppDatabase
import com.deathhit.data.media_progress.data_source.DataSourceModule
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource
import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSourceImp
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
    fun provideMediaItemLocalDataSource(testMediaProgressLocalDataSource: TestMediaProgressLocalDataSource): MediaProgressLocalDataSource =
        testMediaProgressLocalDataSource

    @Provides
    @Singleton
    fun provideMediaProgressLocalDataSourceImp(appDatabase: AppDatabase): MediaProgressLocalDataSourceImp =
        MediaProgressLocalDataSourceImp(appDatabase)

    @Provides
    @Singleton
    fun provideTestMediaItemLocalDataSource(mediaProgressLocalDataSourceImp: MediaProgressLocalDataSourceImp): TestMediaProgressLocalDataSource =
        TestMediaProgressLocalDataSource(mediaProgressLocalDataSourceImp)
}