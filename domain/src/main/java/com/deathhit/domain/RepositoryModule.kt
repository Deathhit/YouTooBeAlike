package com.deathhit.domain

import com.deathhit.domain.repository.video.VideoRepository
import com.deathhit.domain.repository.video.VideoRepositoryImp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideVideoRepository(): VideoRepository = VideoRepositoryImp()
}