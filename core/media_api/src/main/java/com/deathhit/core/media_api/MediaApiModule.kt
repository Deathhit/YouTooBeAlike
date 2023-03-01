package com.deathhit.core.media_api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaApiModule {
    @Provides
    @Singleton
    fun provideMediaApiService(): MediaApiService = MediaApiServiceImp()
}