package com.deathhit.data.media_item.config

import com.deathhit.core.media_api.MediaApiModule
import com.deathhit.core.media_api.MediaApiService
import com.deathhit.core.media_api.test.FakeMediaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MediaApiModule::class]
)
object TestMediaApiModule {
    @Provides
    @Singleton
    fun provideFakeMediaApiService(): FakeMediaApiService = FakeMediaApiService()

    @Provides
    @Singleton
    fun provideImageApiService(fakeMediaApiService: FakeMediaApiService): MediaApiService =
        fakeMediaApiService
}