package com.deathhit.data.media_progress.data_source

import com.deathhit.core.app_database.AppDatabase
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
    fun provideMediaProgressLocalDataSource(appDatabase: AppDatabase): MediaProgressLocalDataSource =
        MediaProgressLocalDataSourceImp(appDatabase)
}