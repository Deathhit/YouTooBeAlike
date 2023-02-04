package com.deathhit.data.media_progress

import com.deathhit.data.media_progress.data_source.MediaProgressLocalDataSource
import com.deathhit.data.media_progress.repository.MediaProgressRepository
import com.deathhit.data.media_progress.repository.MediaProgressRepositoryImp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaProgressRepositoryModule {
    @Provides
    @Singleton
    internal fun provideMediaProgressRepository(mediaProgressLocalDataSource: MediaProgressLocalDataSource): MediaProgressRepository =
        MediaProgressRepositoryImp(mediaProgressLocalDataSource)
}