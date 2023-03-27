package com.deathhit.feature.media_item_list.config

import com.deathhit.domain.MediaItemRepository
import com.deathhit.domain.MediaProgressRepository
import com.deathhit.domain.test.FakeMediaItemRepository
import com.deathhit.domain.test.FakeMediaProgressRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestRepositoryModule {
    @Provides
    fun provideMediaItemRepository(fakeMediaItemRepository: FakeMediaItemRepository): MediaItemRepository = fakeMediaItemRepository

    @Provides
    fun provideMediaProgressRepository(fakeMediaProgressRepository: FakeMediaProgressRepository): MediaProgressRepository =
        fakeMediaProgressRepository

    @Provides
    @Singleton
    fun provideFakeMediaItemRepository(): FakeMediaItemRepository = FakeMediaItemRepository()

    @Provides
    @Singleton
    fun provideFakeMediaProgressRepository(): FakeMediaProgressRepository = FakeMediaProgressRepository()
}